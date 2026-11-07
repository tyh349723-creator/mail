package com.mall.alipay.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.MerchantRate;
import com.mall.alipay.domain.PayChannel;
import com.mall.alipay.mapper.MerchantRateMapper;
import com.mall.alipay.mapper.PayChannelMapper;
import com.mall.alipay.service.PayService;
import com.mall.alipay.service.RefundService;
import com.mall.alipay.service.StatisService;
import com.mall.alipay.service.WalletService;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mall-alipay 内部接口：仅供 mall-mgr 经内部令牌直连调用（/internal/* 已注册鉴权过滤器）。
 * 通道类：关单、退款申请/列表/审核、渠道配置/开关/费率、提现列表/审核、平台与商户统计。
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final PayChannelMapper payChannelMapper;
    private final MerchantRateMapper merchantRateMapper;
    private final PayService payService;
    private final RefundService refundService;
    private final WalletService walletService;
    private final StatisService statisService;

    /** 关单（订单超时/取消时同步关闭支付单，透传关闭原因） */
    @PostMapping("/pay/close")
    public Result payClose(@RequestBody Map<String, Object> body) {
        return payService.closeByOrderNo(s(body, "orderNo"), s(body, "reason"));
    }

    // ---------- 退款 ----------

    /** 退款申请（用户/订单侧发起） */
    @PostMapping("/refund/apply")
    public Result refundApply(@RequestBody Map<String, Object> body) {
        return refundService.apply(body);
    }

    @PostMapping("/refund/list")
    public Result refundList(@RequestBody Map<String, Object> body) {
        return refundService.list(body);
    }

    @PostMapping("/refund/audit")
    public Result refundAudit(@RequestBody Map<String, Object> body) {
        long id = longv(body, "id");
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        return refundService.audit(id, approve, s(body, "reason"));
    }

    /** 撤销退款申请（买家撤销审核中的申请） */
    @PostMapping("/refund/cancel")
    public Result refundCancel(@RequestBody Map<String, Object> body) {
        return refundService.cancel(body);
    }

    // ---------- 渠道配置 ----------

    @PostMapping("/channel/list")
    public Result channelList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PayChannel c : payChannelMapper.selectList(new QueryWrapper<PayChannel>().orderByAsc("id"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("channelCode", c.getChannelCode());
            m.put("channelName", c.getChannelName());
            m.put("baseRate", c.getBaseRate());
            m.put("status", c.getStatus());
            m.put("configJson", c.getConfigJson() == null ? "" : c.getConfigJson());
            list.add(m);
        }
        return Result.ok().add("list", list);
    }

    @PostMapping("/channel/config")
    public Result channelConfig(@RequestBody Map<String, Object> body) {
        long id = longv(body, "id");
        PayChannel c = id > 0 ? payChannelMapper.selectById(id) : null;
        if (c == null) {
            String code = s(body, "channelCode");
            c = payChannelMapper.selectOne(new QueryWrapper<PayChannel>().eq("channel_code", code));
            if (c == null) {
                throw new ApiException("渠道不存在");
            }
        }
        if (body.containsKey("baseRate")) {
            c.setBaseRate(new BigDecimal(String.valueOf(body.get("baseRate"))));
        }
        if (body.containsKey("channelName")) {
            c.setChannelName(s(body, "channelName"));
        }
        if (body.containsKey("configJson")) {
            c.setConfigJson(s(body, "configJson"));
        }
        if (body.containsKey("status")) {
            c.setStatus(intv(body, "status", 1));
        }
        payChannelMapper.updateById(c);
        return Result.ok("渠道配置已保存");
    }

    @PostMapping("/channel/status")
    public Result channelStatus(@RequestBody Map<String, Object> body) {
        long id = longv(body, "id");
        PayChannel c = payChannelMapper.selectById(id);
        if (c == null) {
            throw new ApiException("渠道不存在");
        }
        c.setStatus(intv(body, "status", 1));
        payChannelMapper.updateById(c);
        return Result.ok("渠道状态已更新");
    }

    /** 商户差异化费率配置 */
    @PostMapping("/channel/rate")
    public Result channelRate(@RequestBody Map<String, Object> body) {
        String merchantNo = s(body, "merchantNo");
        String wayCode = s(body, "wayCode");
        if (merchantNo.isBlank() || wayCode.isBlank()) {
            throw new ApiException("参数缺失");
        }
        MerchantRate rate = merchantRateMapper.selectOne(new QueryWrapper<MerchantRate>()
                .eq("merchant_no", merchantNo).eq("way_code", wayCode).last("LIMIT 1"));
        if (rate == null) {
            rate = new MerchantRate();
            rate.setMerchantNo(merchantNo);
            rate.setWayCode(wayCode);
            rate.setMchRate(new BigDecimal(String.valueOf(body.get("mchRate"))));
            merchantRateMapper.insert(rate);
        } else {
            rate.setMchRate(new BigDecimal(String.valueOf(body.get("mchRate"))));
            merchantRateMapper.updateById(rate);
        }
        return Result.ok("费率配置成功");
    }

    /** 商户费率查询（管理端「差异化费率配置」弹窗：3 渠道 × 基础费率/当前费率） */
    @PostMapping("/channel/rate/list")
    public Result channelRateList(@RequestBody Map<String, Object> body) {
        String merchantNo = s(body, "merchantNo");
        List<Map<String, Object>> list = new ArrayList<>();
        if (!merchantNo.isBlank()) {
            for (PayChannel c : payChannelMapper.selectList(new QueryWrapper<PayChannel>().orderByAsc("id"))) {
                MerchantRate mr = merchantRateMapper.selectOne(new QueryWrapper<MerchantRate>()
                        .eq("merchant_no", merchantNo).eq("way_code", c.getChannelCode()).last("LIMIT 1"));
                BigDecimal mchRate = mr == null ? c.getBaseRate() : mr.getMchRate();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("wayCode", c.getChannelCode());
                m.put("channelName", c.getChannelName());
                m.put("baseRate", c.getBaseRate());
                m.put("mchRate", mchRate);
                list.add(m);
            }
        }
        return Result.ok().add("list", list);
    }

    // ---------- 提现 ----------

    @PostMapping("/withdraw/list")
    public Result withdrawList(@RequestBody Map<String, Object> body) {
        return walletService.withdrawListPlatform(body);
    }

    @PostMapping("/withdraw/audit")
    public Result withdrawAudit(@RequestBody Map<String, Object> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        return walletService.withdrawAudit(longv(body, "id"), approve);
    }

    // ---------- 统计 ----------

    @PostMapping("/statis/pay")
    public Result statisPay(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        return statisService.platform(s(b, "begin"), s(b, "end"), s(b, "type"));
    }

    @PostMapping("/statis/mch")
    public Result statisMch(@RequestBody Map<String, Object> body) {
        return statisService.merchant(s(body, "merchantNo"), s(body, "begin"), s(body, "end"));
    }

    private String s(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
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

    private int intv(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}