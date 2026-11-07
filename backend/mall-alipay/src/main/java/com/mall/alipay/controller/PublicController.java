package com.mall.alipay.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.PayChannel;
import com.mall.alipay.mapper.PayChannelMapper;
import com.mall.alipay.service.PayService;
import com.mall.alipay.service.RefundService;
import com.mall.alipay.service.StatisService;
import com.mall.alipay.service.WalletService;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付/退款/钱包/统计 对外接口（经网关 10010 验签 + JWT 鉴权进入）。
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class PublicController {

    private final PayChannelMapper payChannelMapper;
    private final PayService payService;
    private final RefundService refundService;
    private final WalletService walletService;
    private final StatisService statisService;

    /** 可用支付渠道（收银台渠道选择） */
    @PostMapping("/pay/channels")
    public Result channels(@RequestBody(required = false) Map<String, Object> body) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PayChannel c : payChannelMapper.selectList(new QueryWrapper<PayChannel>()
                .eq("status", 1).orderByAsc("id"))) {
            list.add(Map.of("wayCode", c.getChannelCode(), "channelName", c.getChannelName(), "baseRate", c.getBaseRate()));
        }
        return Result.ok().add("list", list);
    }

    /** 统一下单（指导书 /pay/create） */
    @PostMapping("/pay/create")
    public Result payCreate(@RequestBody Map<String, Object> body) {
        return payService.create(body);
    }

    /** 渠道异步回调（指导书 /pay/callback，验签 + 幂等） */
    @PostMapping("/pay/callback")
    public Result payCallback(@RequestBody Map<String, Object> body) {
        return payService.callback(body);
    }

    /** 支付结果查询（指导书 /pay/query） */
    @PostMapping("/pay/query")
    public Result payQuery(@RequestBody Map<String, Object> body) {
        return payService.query(body);
    }

    /** 商户支付单列表（指导书 /pay/list/mch） */
    @PostMapping("/pay/list/mch")
    public Result payListMch(@RequestBody Map<String, Object> body) {
        return payService.listMch(body);
    }

    /** 退款申请（指导书 /refund/create） */
    @PostMapping("/refund/create")
    public Result refundCreate(@RequestBody Map<String, Object> body) {
        return refundService.apply(body);
    }

    /** 退款查询（指导书 /refund/query） */
    @PostMapping("/refund/query")
    public Result refundQuery(@RequestBody Map<String, Object> body) {
        return refundService.query(body);
    }

    /** 商户退款记录（指导书 5.2.3：仅返回当前商户的退款单与处理进度） */
    @PostMapping("/refund/list/mch")
    public Result refundListMch(@RequestBody Map<String, Object> body) {
        return refundService.listMch(body);
    }

    /** 商户处理退款（5.2.3 售后退款管理：同意退款/驳回，处理前须为待处理） */
    @PostMapping("/refund/handle/mch")
    public Result refundHandleMch(@RequestBody Map<String, Object> body) {
        return refundService.handleMch(body);
    }

    // ---------- 商户钱包 ----------

    @PostMapping("/wallet/balance")
    public Result walletBalance(@RequestBody(required = false) Map<String, Object> body) {
        return walletService.balance();
    }

    @PostMapping("/wallet/records")
    public Result walletRecords(@RequestBody Map<String, Object> body) {
        return walletService.records(body);
    }

    @PostMapping("/wallet/withdraw/apply")
    public Result withdrawApply(@RequestBody Map<String, Object> body) {
        return walletService.withdrawApply(body);
    }

    @PostMapping("/wallet/withdraw/list")
    public Result withdrawList(@RequestBody Map<String, Object> body) {
        return walletService.withdrawList(body);
    }

    // ---------- 商户经营统计（指导书 /statis/mch） ----------

    @PostMapping("/statis/mch")
    public Result statisMch(@RequestBody(required = false) Map<String, Object> body) {
        if (!CurrentUser.isMerchant() || CurrentUser.userNum() == null) {
            return com.mall.common.result.Result.err("401", "请以商户身份登录");
        }
        // 可选：begin/end（yyyy-MM-dd）自定义统计时间范围
        Map<String, Object> b = body == null ? Map.of() : body;
        String begin = b.get("begin") == null ? "" : String.valueOf(b.get("begin"));
        String end = b.get("end") == null ? "" : String.valueOf(b.get("end"));
        return statisService.merchant(CurrentUser.userNum(), begin, end);
    }
}