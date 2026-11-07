package com.mall.alipay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * mall-alipay → mall-mgr 内部回调客户端（直连内部端口 + 内部令牌头）。
 * <p>支付成功回调 / 退款完成通知主流程：渠道侧落地 → 通知业务侧置订单状态。</p>
 */
@Component
public class MgrClient {

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public MgrClient(@Value("${mall.internal.mgr-base}") String base,
                     @Value("${mall.internal.token}") String token) {
        this.client = RestClient.builder().baseUrl(base)
                .defaultHeader("X-Internal-Token", token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** 通知业务侧订单支付成功（幂等，业务侧状态机保证） */
    public void notifyMarkPaid(String orderNo, String channel, String payTime) {
        try {
            client.post().uri("/internal/order/mark-paid")
                    .body(Map.of("orderNo", orderNo, "channel", channel, "payTime", payTime))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log("支付通知 mgr 失败：" + orderNo + " " + e.getMessage());
        }
    }

    /** 通知业务侧退款完成（幂等） */
    public void notifyRefund(String orderNo) {
        try {
            client.post().uri("/internal/order/refund-notify")
                    .body(Map.of("orderNo", orderNo))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log("退款通知 mgr 失败：" + orderNo + " " + e.getMessage());
        }
    }

    private void log(String msg) {
        System.out.println("[mall-alipay] " + msg);
    }
}