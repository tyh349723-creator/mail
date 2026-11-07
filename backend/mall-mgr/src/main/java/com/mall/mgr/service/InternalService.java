package com.mall.mgr.service;

import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * mall-mgr 内部回调接口（仅允许 mall-alipay 经内部令牌调用）。
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalService {

    private final OrderService orderService;

    /** 支付成功回调（alipay → mgr）：订单置已支付 */
    @PostMapping("/order/mark-paid")
    public Result markPaid(@RequestBody Map<String, Object> body) {
        String orderNo = String.valueOf(body.getOrDefault("orderNo", ""));
        boolean ok = orderService.markPaid(orderNo,
                String.valueOf(body.getOrDefault("channel", "")),
                String.valueOf(body.getOrDefault("payTime", "")));
        return ok ? Result.ok("处理成功") : Result.err("1", "订单状态异常或不存在");
    }

    /** 退款成功通知（alipay → mgr）：订单置已退款 */
    @PostMapping("/order/refund-notify")
    public Result refundNotify(@RequestBody Map<String, Object> body) {
        String orderNo = String.valueOf(body.getOrDefault("orderNo", ""));
        orderService.refundNotify(orderNo);
        return Result.ok("处理成功");
    }
}