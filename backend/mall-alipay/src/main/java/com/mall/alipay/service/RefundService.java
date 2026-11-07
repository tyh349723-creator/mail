package com.mall.alipay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.PayOrder;
import com.mall.alipay.domain.Refund;
import com.mall.alipay.mapper.PayOrderMapper;
import com.mall.alipay.mapper.RefundMapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.common.util.NoGen;
import com.mall.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 退款服务：退款申请（业务侧订单发起 / 支付侧 /refund/create）、查询、平台审核。
 * 审核通过 → 钱包扣回 + 通知业务侧订单置已退款；驳回 → 状态置失败。
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PayOrderMapper payOrderMapper;
    private final RefundMapper refundMapper;
    private final WalletService walletService;
    private final MgrClient mgrClient;
    private final AlipayGatewayService alipayGateway;

    /** 发起退款申请（幂等：同一订单同一渠道重复申请去重） */
    public Result apply(Map<String, Object> body) {
        String orderNo = required(body, "orderNo");
        String merchantNo = required(body, "merchantNo");
        BigDecimal money = new BigDecimal(String.valueOf(body.get("refundMoney")));
        String reason = body.get("refundReason") == null ? "" : String.valueOf(body.get("refundReason"));
        if (money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("退款金额不合法");
        }
        // 校验已支付金额与可退余额
        BigDecimal paid = payOrderMapper.selectList(new QueryWrapper<PayOrder>()
                        .eq("order_no", orderNo).eq("status", StatusConst.PAY_SUCCESS)).stream()
                .map(PayOrder::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("订单未支付成功，无法退款");
        }
        BigDecimal refunded = refundMapper.selectList(new QueryWrapper<Refund>()
                        .eq("order_no", orderNo)
                        .in("status", StatusConst.REFUND_PENDING, StatusConst.REFUND_SUCCESS))
                .stream().map(Refund::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refunded.add(money).compareTo(paid) > 0) {
            throw new ApiException("退款金额超过可退余额");
        }
        // 防止同一订单重复发起相同金额的审核中退款
        Refund dup = refundMapper.selectOne(new QueryWrapper<Refund>()
                .eq("order_no", orderNo).eq("money", money)
                .eq("status", StatusConst.REFUND_PENDING).last("LIMIT 1"));
        if (dup != null) {
            throw new ApiException("已存在待处理的退款申请");
        }
        Refund r = new Refund();
        r.setRefundOrderId(NoGen.next("RF"));
        r.setOrderNo(orderNo);
        r.setMerchantNo(merchantNo);
        r.setMoney(money);
        r.setReason(reason.isBlank() ? "不想要了" : reason);
        r.setStatus(StatusConst.REFUND_PENDING);
        refundMapper.insert(r);
        return Result.ok("退款申请已提交，等待商户处理").add("refundOrderId", r.getRefundOrderId());
    }

    /** 平台退款列表（分页，orderNo/商户/状态/金额区间筛选：amountMin/amountMax 供「大额退款审核」用） */
    public Result list(Map<String, Object> body) {
        String orderNo = str(body, "orderNo");
        String merchantNo = str(body, "merchantNo");
        int status = intv(body, "status", -99);
        BigDecimal amountMin = decv(body, "amountMin", null);
        BigDecimal amountMax = decv(body, "amountMax", null);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<Refund> w = new QueryWrapper<Refund>()
                .like(!orderNo.isBlank(), "order_no", orderNo)
                .eq(!merchantNo.isBlank(), "merchant_no", merchantNo)
                .eq(status != -99, "status", status)
                .ge(amountMin != null, "money", amountMin)
                .le(amountMax != null, "money", amountMax)
                .orderByDesc("create_time");
        long total = refundMapper.selectCount(w);
        List<Refund> list = refundMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::view).toList();
        return Result.ok().page(total, current, size, records);
    }

    /**
     * 商户退款记录（/refund/list/mch：5.2.3 售后退款管理）。
     * 只返回当前登录商户（CurrentUser）的退款单，含审核状态/渠道退款进度/金额/原因。
     */
    public Result listMch(Map<String, Object> body) {
        String shopNum = currentShopNum();
        String orderNo = str(body, "orderNo");
        int status = intv(body, "status", -99);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<Refund> w = new QueryWrapper<Refund>()
                .eq("merchant_no", shopNum)
                .like(!orderNo.isBlank(), "order_no", orderNo)
                .eq(status != -99, "status", status)
                .orderByDesc("create_time");
        long total = refundMapper.selectCount(w);
        List<Refund> list = refundMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::view).toList();
        return Result.ok().page(total, current, size, records);
    }

    /** 平台兜底审核：同意 → 退款；驳回 → 状态置失败 */
    public Result audit(long id, boolean approve, String reason) {
        Refund r = refundMapper.selectById(id);
        if (r == null) {
            throw new ApiException("退款单不存在");
        }
        return doHandle(r, approve, reason, "平台审核通过，已退款", "不符合退款条件");
    }

    /**
     * 商户处理退款（/refund/handle/mch，5.2.3 售后退款管理）。
     * 仅当前商户（CurrentUser）自己的退款单可处理；同意 → 钱包扣回 + 通知业务侧订单置已退款，
     * 驳回 → 状态置失败。处理前状态必须为待处理。
     */
    public Result handleMch(Map<String, Object> body) {
        String shopNum = currentShopNum();
        long id = longv(body, "id");
        Refund r = refundMapper.selectOne(new QueryWrapper<Refund>()
                .eq("id", id).eq("merchant_no", shopNum).last("LIMIT 1"));
        if (r == null) {
            throw new ApiException("退款单不存在");
        }
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        return doHandle(r, approve, str(body, "reason"), "商户同意退款，已处理", "商户驳回退款申请");
    }

    /** 处理退款单（同意付款/驳回置失败），仅 待处理 可处理；供平台审核与商户处理共用 */
    private Result doHandle(Refund r, boolean approve, String reason, String okRemark, String defaultRejectRemark) {
        if (r.getStatus() != StatusConst.REFUND_PENDING) {
            throw new ApiException("退款单已处理");
        }
        if (approve) {
            // 先扣钱包再置已处理：钱包余额不足会抛异常，此时退款单保持待处理
            walletService.refundOut(r.getMerchantNo(), r.getMoney(), r.getOrderNo());
            // 真实支付宝支付：同步发起原路退款；失败则回补钱包并保持待处理，可稍后重试
            PayOrder paid = payOrderMapper.selectOne(new QueryWrapper<PayOrder>()
                    .eq("order_no", r.getOrderNo())
                    .eq("channel_code", "ALIPAY")
                    .eq("status", StatusConst.PAY_SUCCESS)
                    .last("LIMIT 1"));
            if (paid != null && alipayGateway.enabled()
                    && !alipayGateway.refund(paid.getPayOrderId(), r.getRefundOrderId(), r.getMoney(), r.getReason())) {
                walletService.credit(r.getMerchantNo(), r.getMoney(), BigDecimal.ZERO, "退款失败回补 " + r.getOrderNo());
                throw new ApiException("支付宝退款失败，请稍后重试");
            }
            mgrClient.notifyRefund(r.getOrderNo());
            r.setStatus(StatusConst.REFUND_SUCCESS);
            r.setAuditRemark(reason == null || reason.isBlank() ? okRemark : reason);
        } else {
            r.setStatus(StatusConst.REFUND_FAILED);
            r.setAuditRemark(reason == null || reason.isBlank() ? defaultRejectRemark : reason);
        }
        r.setAuditTime(TimeUtils.now());
        refundMapper.updateById(r);
        return Result.ok(approve ? "退款已处理" : "已驳回");
    }

    /** 退款查询：单条（refundOrderId / orderNo） */
    public Result query(Map<String, Object> body) {
        String refundOrderId = str(body, "refundOrderId");
        String orderNo = str(body, "orderNo");
        List<Refund> list = refundMapper.selectList(new QueryWrapper<Refund>()
                .eq(!refundOrderId.isBlank(), "refund_order_id", refundOrderId)
                .eq(!orderNo.isBlank(), "order_no", orderNo)
                .orderByDesc("create_time"));
        if (list.isEmpty()) {
            throw new ApiException("退款单不存在");
        }
        return Result.ok().add("refund", view(list.get(0)));
    }

    private Map<String, Object> view(Refund r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("refundOrderId", r.getRefundOrderId());
        m.put("orderNo", r.getOrderNo());
        m.put("merchantNo", r.getMerchantNo());
        m.put("money", r.getMoney());
        m.put("reason", r.getReason());
        m.put("status", r.getStatus());
        m.put("statusText", refundStatusText(r.getStatus()));
        m.put("auditRemark", r.getAuditRemark() == null ? "" : r.getAuditRemark());
        m.put("createTime", r.getCreateTime());
        m.put("auditTime", r.getAuditTime() == null ? "" : r.getAuditTime());
        return m;
    }

    private String refundStatusText(int status) {
        return switch (status) {
            case StatusConst.REFUND_PENDING -> "待处理";
            case StatusConst.REFUND_SUCCESS -> "已处理";
            case StatusConst.REFUND_FAILED -> "退款失败";
            case StatusConst.REFUND_CANCELED -> "已撤销";
            default -> "未知";
        };
    }

    /** 撤销退款申请（买家主动撤销待审核的申请，仅审核中可撤销） */
    public Result cancel(Map<String, Object> body) {
        String orderNo = required(body, "orderNo");
        Refund r = refundMapper.selectOne(new QueryWrapper<Refund>()
                .eq("order_no", orderNo).eq("status", StatusConst.REFUND_PENDING).last("LIMIT 1"));
        if (r == null) {
            throw new ApiException("无可撤销的退款申请");
        }
        r.setStatus(StatusConst.REFUND_CANCELED);
        r.setAuditRemark("买家已撤销退款申请");
        r.setAuditTime(TimeUtils.now());
        refundMapper.updateById(r);
        return Result.ok("退款申请已撤销");
    }

    private String currentShopNum() {
        if (!CurrentUser.isMerchant()) {
            throw ApiException.unauthorized();
        }
        return CurrentUser.userNum();
    }

    private BigDecimal decv(Map<String, Object> body, String key, BigDecimal def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String required(Map<String, Object> body, String key) {
        String v = str(body, key);
        if (v.isBlank()) {
            throw new ApiException("参数缺失：" + key);
        }
        return v;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private int intv(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long longv(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return 0;
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}