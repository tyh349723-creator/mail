package com.mall.alipay.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.alipay.config.AlipayProperties;
import com.mall.alipay.domain.PayChannel;
import com.mall.alipay.mapper.PayChannelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付宝网关封装：手机网站支付（alipay.trade.wap.pay）、异步通知验签、交易查询、原路退款。
 * <p>凭证来源：管理端「支付配置 → 支付宝」写入的 {@code pay_channel.config_json}
 * （appId / merchantPrivateKey / alipayPublicKey / notifyUrl / gatewayUrl）优先，
 * 缺省回退到 application.yml 的 mall.alipay.*。三项凭证不齐时 {@link #enabled()} 为 false，
 * 支付链路自动回退到本地模拟收银台，保证无密钥也能演示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayGatewayService {

    private static final String CHANNEL_CODE = "ALIPAY";
    private static final String PRODUCT_CODE = "QUICK_WAP_WAY";

    private final PayChannelMapper payChannelMapper;
    private final AlipayProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 按配置指纹缓存 SDK 客户端，避免每次请求重复构造 */
    private final Map<String, AlipayClient> clientCache = new ConcurrentHashMap<>();

    /** 当前生效的渠道配置 */
    public record AlipayConfig(String appId, String privateKey, String alipayPublicKey,
                               String gatewayUrl, String notifyUrl, String returnUrl,
                               String signType, String charset) {
        public boolean enabled() {
            return notBlank(appId) && notBlank(privateKey) && notBlank(alipayPublicKey);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    /** 读取渠道配置并合并 application.yml 兜底 */
    public AlipayConfig config() {
        Map<String, Object> db = readChannelConfig();
        String dbAppId = text(db, "appId");
        String dbPrivateKey = text(db, "merchantPrivateKey");
        String dbPublicKey = text(db, "alipayPublicKey");
        // 管理端三项凭证齐全才以渠道配置为准；否则整体回退 application.yml（避免种子数据里的占位 appId 覆盖真实配置）
        boolean dbComplete = !dbAppId.isBlank() && !dbPrivateKey.isBlank() && !dbPublicKey.isBlank();
        String appId = dbComplete ? dbAppId : props.getAppId();
        String privateKey = dbComplete ? dbPrivateKey : props.getPrivateKey();
        String alipayPublicKey = dbComplete ? dbPublicKey : props.getAlipayPublicKey();
        String gatewayUrl = first(text(db, "gatewayUrl"), props.getGatewayUrl());
        String notifyUrl = first(text(db, "notifyUrl"), props.getNotifyUrl());
        String returnUrl = first(text(db, "returnUrl"), props.getReturnUrl());
        return new AlipayConfig(appId, cleanKey(privateKey), cleanKey(alipayPublicKey),
                gatewayUrl, notifyUrl, returnUrl,
                first(text(db, "signType"), props.getSignType()),
                first(text(db, "charset"), props.getCharset()));
    }

    /** 真实支付是否可用（AppID + 应用私钥 + 支付宝公钥齐全） */
    public boolean enabled() {
        return config().enabled();
    }

    /** 当前 AppID（用于校验异步通知中的 app_id） */
    public String appId() {
        return config().appId();
    }

    /**
     * 手机网站支付：返回支付宝自动提交的 HTML 表单；失败返回 null（调用方回退模拟收银台）。
     */
    public String createWapPayForm(String payOrderId, BigDecimal money, String subject, String quitUrl) {
        AlipayConfig c = config();
        if (!c.enabled()) {
            return null;
        }
        try {
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(c.notifyUrl());
            request.setReturnUrl(withParam(c.returnUrl(), "payOrderId", payOrderId));

            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(payOrderId);
            model.setTotalAmount(money.setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setSubject(subject);
            model.setProductCode(PRODUCT_CODE);
            model.setTimeoutExpress("15m");
            model.setQuitUrl(quitUrl);
            request.setBizModel(model);

            AlipayTradeWapPayResponse resp = client(c).pageExecute(request);
            if (resp.isSuccess() && resp.getBody() != null) {
                return resp.getBody();
            }
            log.warn("支付宝下单失败 code={} msg={} sub={}", resp.getCode(), resp.getMsg(), resp.getSubMsg());
        } catch (Exception e) {
            log.error("支付宝下单异常 payOrderId={}", payOrderId, e);
        }
        return null;
    }

    /** 异步通知 / 同步跳回 RSA2 验签 */
    public boolean verify(Map<String, String> params) {
        AlipayConfig c = config();
        if (!c.enabled()) {
            return false;
        }
        try {
            return AlipaySignature.rsaCheckV1(params, c.alipayPublicKey(), c.charset(), c.signType());
        } catch (AlipayApiException e) {
            log.warn("支付宝回调验签异常: {}", e.getMessage());
            return false;
        }
    }

    /** 主动查单，返回 trade_status（TRADE_SUCCESS / TRADE_FINISHED 等）；查不到或异常返回 null */
    public String queryTrade(String payOrderId) {
        AlipayConfig c = config();
        if (!c.enabled()) {
            return null;
        }
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(payOrderId);
            request.setBizModel(model);
            AlipayTradeQueryResponse resp = client(c).execute(request);
            return resp.isSuccess() ? resp.getTradeStatus() : null;
        } catch (Exception e) {
            log.warn("支付宝查单异常 payOrderId={}: {}", payOrderId, e.getMessage());
            return null;
        }
    }

    /** 原路退款，成功返回 true */
    public boolean refund(String payOrderId, String refundOrderId, BigDecimal money, String reason) {
        AlipayConfig c = config();
        if (!c.enabled()) {
            return false;
        }
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(payOrderId);
            model.setOutRequestNo(refundOrderId);
            model.setRefundAmount(money.setScale(2, RoundingMode.HALF_UP).toPlainString());
            model.setRefundReason(reason);
            request.setBizModel(model);
            AlipayTradeRefundResponse resp = client(c).execute(request);
            if (resp.isSuccess()) {
                return true;
            }
            log.warn("支付宝退款失败 refundOrderId={} code={} msg={} sub={}",
                    refundOrderId, resp.getCode(), resp.getMsg(), resp.getSubMsg());
            return false;
        } catch (Exception e) {
            log.error("支付宝退款异常 refundOrderId={}", refundOrderId, e);
            return false;
        }
    }

    // ---------- 内部工具 ----------

    private AlipayClient client(AlipayConfig c) {
        String key = c.appId() + "|" + c.gatewayUrl() + "|" + c.privateKey().hashCode() + "|" + c.alipayPublicKey().hashCode();
        return clientCache.computeIfAbsent(key, k -> new DefaultAlipayClient(
                c.gatewayUrl(), c.appId(), c.privateKey(), "json", c.charset(), c.alipayPublicKey(), c.signType()));
    }

    /** 读取 pay_channel.config_json（解析失败按空配置处理） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readChannelConfig() {
        PayChannel channel = payChannelMapper.selectOne(new QueryWrapper<PayChannel>()
                .eq("channel_code", CHANNEL_CODE).last("LIMIT 1"));
        if (channel == null || channel.getConfigJson() == null || channel.getConfigJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(channel.getConfigJson(), Map.class);
        } catch (Exception e) {
            log.warn("支付宝渠道配置 JSON 解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String text(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static String first(String a, String b) {
        return (a == null || a.isBlank()) ? (b == null ? "" : b) : a;
    }

    /** 去掉 PEM 头尾与空白，SDK 需要 Base64 主体 */
    private static String cleanKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private static String withParam(String url, String name, String value) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url + (url.contains("?") ? "&" : "?") + name + "=" + value;
    }
}
