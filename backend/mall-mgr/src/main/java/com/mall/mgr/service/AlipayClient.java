package com.mall.mgr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * mall-mgr → mall-alipay 内部调用客户端（直连内部端口 + 内部令牌头，不经网关验签）。
 * <p>管理端在 mgr 聚合展示支付/退款/渠道/钱包数据，具体业务归属 mall-alipay。</p>
 */
@Component
public class AlipayClient {

    private final RestClient client;
    private final String internalToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public AlipayClient(@Value("${mall.internal.alipay-base}") String base,
                        @Value("${mall.internal.token}") String token) {
        this.internalToken = token;
        this.client = RestClient.builder().baseUrl(base).build();
    }

    /** 统一 POST 内部接口，返回响应 JSON（Map） */
    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            String json = client.post().uri(path)
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", internalToken)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return json == null ? Map.of() : mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("id", "500", "message", "支付服务不可用");
        }
    }

    /** 关闭支付单（超时关单/取消订单时同步，透传关闭原因） */
    public Map<String, Object> closePayOrder(String orderNo, String reason) {
        return post("/internal/pay/close", Map.of("orderNo", orderNo, "reason", reason == null ? "" : reason));
    }

    /** 用户/商户发起退款申请（归属支付侧） */
    public Map<String, Object> refundApply(String orderNo, String merchantNo, String refundMoney, String reason) {
        return post("/internal/refund/apply",
                Map.of("orderNo", orderNo, "merchantNo", merchantNo, "refundMoney", refundMoney, "refundReason", reason));
    }

    public Map<String, Object> refundList(Map<String, Object> param) {
        return post("/internal/refund/list", param);
    }

    /** 撤销退款申请（买家撤销审核中的申请） */
    public Map<String, Object> refundCancel(String orderNo) {
        return post("/internal/refund/cancel", Map.of("orderNo", orderNo));
    }

    public Map<String, Object> refundAudit(Long id, boolean approve, String reason) {
        return post("/internal/refund/audit", Map.of("id", id, "approve", approve, "reason", reason == null ? "" : reason));
    }

    public Map<String, Object> channelList() {
        return post("/internal/channel/list", Map.of());
    }

    public Map<String, Object> channelConfig(Map<String, Object> param) {
        return post("/internal/channel/config", param);
    }

    public Map<String, Object> channelStatus(Long id, int status) {
        return post("/internal/channel/status", Map.of("id", id, "status", status));
    }

    public Map<String, Object> channelRate(String merchantNo, String wayCode, String mchRate) {
        return post("/internal/channel/rate", Map.of("merchantNo", merchantNo, "wayCode", wayCode, "mchRate", mchRate));
    }

    /** 商户差异化费率查询（管理端费率配置弹窗，3 渠道 × 基础费率/当前费率） */
    public Map<String, Object> merchantRateList(String merchantNo) {
        return post("/internal/channel/rate/list", Map.of("merchantNo", merchantNo == null ? "" : merchantNo));
    }

    public Map<String, Object> withdrawList(Map<String, Object> param) {
        return post("/internal/withdraw/list", param);
    }

    public Map<String, Object> withdrawAudit(Long id, boolean approve) {
        return post("/internal/withdraw/audit", Map.of("id", id, "approve", approve));
    }

    /** 平台支付侧统计（总额/笔数/渠道占比/趋势），支持 begin/end 区间与 type 趋势粒度 */
    public Map<String, Object> statisPay(String begin, String end, String type) {
        return post("/internal/statis/pay", Map.of(
                "begin", begin == null ? "" : begin,
                "end", end == null ? "" : end,
                "type", type == null ? "" : type));
    }

    /** 平台支付侧统计（全量 + 近 7 日趋势） */
    public Map<String, Object> statisPay() {
        return statisPay("", "", "day");
    }

    /** 商户经营交易统计（营收/手续费/净收益/渠道/趋势），支持 begin/end 区间 */
    public Map<String, Object> statisMchPay(String merchantNo, String begin, String end) {
        return post("/internal/statis/mch", Map.of(
                "merchantNo", merchantNo,
                "begin", begin == null ? "" : begin,
                "end", end == null ? "" : end));
    }

    /** 商户经营交易统计（全量） */
    public Map<String, Object> statisMchPay(String merchantNo) {
        return statisMchPay(merchantNo, "", "");
    }
}