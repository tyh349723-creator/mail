package com.mall.alipay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝开放平台配置（application.yml 的 mall.alipay.*，支持 MALL_ALIPAY_* 环境变量）。
 * <p>管理端「支付配置 → 支付宝」填写的渠道参数会优先于这里的配置；两者都缺凭证时支付退化为本地模拟收银台。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.alipay")
public class AlipayProperties {

    /** 开放平台网关：默认沙箱，生产改为 https://openapi.alipay.com/gateway.do */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 应用 AppID（沙箱用沙箱 AppID） */
    private String appId = "";

    /** 应用私钥（RSA2 / PKCS8，单行 base64 或 PEM 均可） */
    private String privateKey = "";

    /** 支付宝公钥（平台生成，非应用公钥） */
    private String alipayPublicKey = "";

    /** 异步通知地址（需公网可达） */
    private String notifyUrl = "";

    /** 同步跳回地址 */
    private String returnUrl = "";

    /** 签名类型：RSA2 */
    private String signType = "RSA2";

    /** 字符集 */
    private String charset = "UTF-8";
}
