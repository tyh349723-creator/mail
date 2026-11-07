package com.mall.alipay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.*;
import com.mall.alipay.mapper.MerchantRateMapper;
import com.mall.alipay.mapper.PayChannelMapper;
import com.mall.alipay.mapper.PayLogMapper;
import com.mall.alipay.mapper.PayOrderMapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.redis.RedisTools;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.common.util.NoGen;
import com.mall.common.util.SignUtil;
import com.mall.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 支付服务：统一下单 /pay/create、模拟渠道回调（幂等：Redis 锁 + 唯一支付单 + 状态机）、
 * /pay/query、商户支付单列表 /pay/list/mch、关单。
 * <p><b>重点考核点-回调幂等</b>：①Redis 分布式锁保证并发下串行处理同一支付单；②payOrderId 全局唯一（沉降为
 * 内存唯一索引）；③状态机只允许 待支付→支付成功 单向流转，重复回调直接返回成功但不重复入账。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    private final PayChannelMapper payChannelMapper;
    private final MerchantRateMapper merchantRateMapper;
    private final PayOrderMapper payOrderMapper;
    private final PayLogMapper payLogMapper;
    private final RedisTools redisTools;
    private final MgrClient mgrClient;
    private final WalletService walletService;
    private final AlipayGatewayService alipayGateway;

    @Value("${mall.cashier.base-url}")
    private String cashierBaseUrl;

    // ---------- 统一下单 ----------

    /** 下单：{orderNo, merchantNo, money, wayCode} → 生成支付单与收银台地址 */
    public Result create(Map<String, Object> body) {
        String orderNo = requiredStr(body, "orderNo");
        String merchantNo = requiredStr(body, "merchantNo");
        String wayCode = requiredStr(body, "wayCode");
        BigDecimal money = BigDecimal.valueOf(Double.parseDouble(String.valueOf(body.get("money"))));
        if (money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("支付金额不合法");
        }
        PayChannel channel = payChannelMapper.selectOne(new QueryWrapper<PayChannel>().eq("channel_code", wayCode));
        if (channel == null) {
            throw new ApiException("支付渠道不存在");
        }
        if (channel.getStatus() != 1) {
            throw new ApiException("该支付渠道已关闭");
        }
        PayOrder po = new PayOrder();
        po.setPayOrderId(NoGen.next("PO"));
        po.setOrderNo(orderNo);
        po.setMerchantNo(merchantNo);
        po.setChannelCode(wayCode);
        po.setMoney(money);
        po.setFee(BigDecimal.ZERO);
        po.setStatus(StatusConst.PAY_WAIT);
        payOrderMapper.insert(po);
        payLogMapper.insert(event(po, "create", "统一下单 金额=" + money));

        Result r = Result.ok("下单成功");
        // 支付宝凭证齐全 → 真实收银台；否则本地模拟收银台（无密钥也能演示）
        boolean realPay = "ALIPAY".equals(wayCode) && alipayGateway.enabled();
        r.add("payOrderId", po.getPayOrderId());
        r.add("orderNo", orderNo);
        r.add("money", money);
        r.add("wayCode", wayCode);
        r.add("channelName", channel.getChannelName());
        r.add("payMode", realPay ? "real" : "mock");
        r.add("payUrl", cashierBaseUrl + (realPay ? "/pay/alipay/redirect/" : "/pay/cashier/") + po.getPayOrderId());
        return r;
    }

    /**
     * 支付成功核心处理（幂等）。本地模拟渠道回调与正式 /pay/callback 共用。
     * @return true=本次完成支付；false=重复回调/非待支付（幂等返回）
     */
    public boolean onPaySuccess(String payOrderId) {
        // ① 分布式锁（Redis 不可用时自动降级为进程内锁）
        String lock = redisTools.lock("pay:" + payOrderId, 30);
        if (lock == null) {
            throw new ApiException("支付处理中，请勿重复提交");
        }
        try {
            PayOrder po = payOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("pay_order_id", payOrderId));
            if (po == null) {
                throw new ApiException("支付单不存在");
            }
            if (po.getStatus() == StatusConst.PAY_SUCCESS) {
                return false; // 幂等：已成功，直接返回
            }
            if (po.getStatus() != StatusConst.PAY_WAIT) {
                throw new ApiException("支付单当前状态不可支付");
            }
            // ② 手续费（按交易额比例，渠道基础费率 / 商户差异化费率）
            BigDecimal rate = rateOf(po);
            BigDecimal fee = po.getMoney().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            // ③ 入账：钱包 = 交易额 - 手续费
            walletService.credit(po.getMerchantNo(), po.getMoney(), fee, po.getOrderNo());

            po.setFee(fee);
            po.setStatus(StatusConst.PAY_SUCCESS);
            po.setPayTime(TimeUtils.now());
            payOrderMapper.updateById(po);
            payLogMapper.insert(event(po, "callback", "支付成功 实收=" + po.getMoney().subtract(fee) + " 手续费=" + fee));

            // ④ 通知业务侧订单置已支付（状态机保证幂等）
            mgrClient.notifyMarkPaid(po.getOrderNo(), po.getChannelCode(), po.getPayTime());
            return true;
        } finally {
            redisTools.unlock("pay:" + payOrderId, lock);
        }
    }

    /** 商户差异化费率优先，其次渠道基础费率 */
    private BigDecimal rateOf(PayOrder po) {
        MerchantRate mr = merchantRateMapper.selectOne(new QueryWrapper<MerchantRate>()
                .eq("merchant_no", po.getMerchantNo())
                .eq("way_code", po.getChannelCode())
                .last("LIMIT 1"));
        if (mr != null) {
            return mr.getMchRate();
        }
        PayChannel c = payChannelMapper.selectOne(new QueryWrapper<PayChannel>()
                .eq("channel_code", po.getChannelCode()).last("LIMIT 1"));
        return c != null ? c.getBaseRate() : new BigDecimal("0.006");
    }

    /** 正式渠道异步回调 /pay/callback（指导书接口，验签后幂等处理） */
    public Result callback(Map<String, Object> body) {
        if (!SignUtil.checkTimestamp(body)) {
            return Result.err("403", "回调时间戳校验失败");
        }
        if (!SignUtil.verify(body, StatusConst.SIGN_SECRET)) {
            return Result.err("403", "回调签名校验失败");
        }
        String payOrderId = requiredStr(body, "payOrderId");
        boolean done = onPaySuccess(payOrderId);
        return Result.ok(done ? "支付成功" : "重复回调，已忽略");
    }

    // ---------- 查询 / 列表 / 关单 ----------

    public Result query(Map<String, Object> body) {
        String payOrderId = str(body, "payOrderId");
        String orderNo = str(body, "orderNo");
        List<PayOrder> list = payOrderMapper.selectList(new QueryWrapper<PayOrder>()
                .eq(!payOrderId.isBlank(), "pay_order_id", payOrderId)
                .eq(!orderNo.isBlank(), "order_no", orderNo)
                .orderByDesc("create_time"));
        if (list.isEmpty()) {
            throw new ApiException("支付单不存在");
        }
        PayOrder po = list.get(0);
        // 真实支付宝支付：待支付时主动查单兜底（异步通知未送达/本地无法公网回调时也能确认支付）
        if (po.getStatus() == StatusConst.PAY_WAIT && "ALIPAY".equals(po.getChannelCode()) && alipayGateway.enabled()) {
            String tradeStatus = alipayGateway.queryTrade(po.getPayOrderId());
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                try {
                    onPaySuccess(po.getPayOrderId());
                } catch (Exception e) {
                    log.warn("查单补单失败 payOrderId={}: {}", po.getPayOrderId(), e.getMessage());
                }
                po = payOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("pay_order_id", po.getPayOrderId()));
            }
        }
        return Result.ok().add("payOrder", view(po));
    }

    /** 商户支付单列表（指导书 /pay/list/mch：分页，含渠道筛选） */
    public Result listMch(Map<String, Object> body) {
        String shopNum = currentShopNum();
        String wayCode = str(body, "wayCode");
        int status = intv(body, "status", -99);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<PayOrder> w = new QueryWrapper<PayOrder>()
                .eq("merchant_no", shopNum)
                .eq(!wayCode.isBlank(), "channel_code", wayCode)
                .eq(status != -99, "status", status)
                .orderByDesc("create_time");
        long total = payOrderMapper.selectCount(w);
        List<PayOrder> list = payOrderMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::view).toList();
        return Result.ok().page(total, current, size, records);
    }

    /** 关闭支付单（超时关单/取消订单时由业务侧同步调用，记录关闭原因） */
    public Result closeByOrderNo(String orderNo, String reason) {
        String closeReason = (reason == null || reason.isBlank()) ? "订单关闭" : reason;
        List<PayOrder> list = payOrderMapper.selectList(new QueryWrapper<PayOrder>()
                .eq("order_no", orderNo).eq("status", StatusConst.PAY_WAIT));
        for (PayOrder po : list) {
            po.setStatus(StatusConst.PAY_CLOSED);
            po.setCloseTime(TimeUtils.now());
            po.setCloseReason(closeReason);
            payOrderMapper.updateById(po);
            payLogMapper.insert(event(po, "close", "订单关闭，支付单同步关闭：" + closeReason));
        }
        return Result.ok("支付单已关闭");
    }

    private PayLog event(PayOrder po, String event, String remark) {
        PayLog l = new PayLog();
        l.setPayOrderId(po.getPayOrderId());
        l.setOrderNo(po.getOrderNo());
        l.setChannelCode(po.getChannelCode());
        l.setEvent(event);
        l.setRemark(remark);
        return l;
    }

    private Map<String, Object> view(PayOrder p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payOrderId", p.getPayOrderId());
        m.put("orderNo", p.getOrderNo());
        m.put("merchantNo", p.getMerchantNo());
        m.put("wayCode", p.getChannelCode());
        PayChannel ch = payChannelMapper.selectOne(new QueryWrapper<PayChannel>()
                .eq("channel_code", p.getChannelCode()).last("LIMIT 1"));
        m.put("channelName", ch == null ? "" : ch.getChannelName());
        m.put("money", p.getMoney());
        m.put("fee", p.getFee());
        m.put("status", p.getStatus());
        m.put("statusText", payStatusText(p.getStatus()));
        m.put("payTime", p.getPayTime() == null ? "" : p.getPayTime());
        m.put("createTime", p.getCreateTime());
        return m;
    }

    private String payStatusText(int status) {
        return switch (status) {
            case StatusConst.PAY_WAIT -> "待支付";
            case StatusConst.PAY_SUCCESS -> "支付成功";
            case StatusConst.PAY_FAILED -> "支付失败";
            case StatusConst.PAY_REFUNDED -> "已退款";
            case StatusConst.PAY_CLOSED -> "已关闭";
            default -> "未知";
        };
    }

    private String currentShopNum() {
        if (!CurrentUser.isMerchant()) {
            throw com.mall.common.exception.ApiException.unauthorized();
        }
        return CurrentUser.userNum();
    }

    private String requiredStr(Map<String, Object> body, String key) {
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

    /** 七大日期窗口（供统计复用） */
    static List<String> lastSevenDays() {
        List<String> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            days.add(LocalDate.now().minusDays(i).toString());
        }
        return days;
    }
}