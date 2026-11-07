package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付渠道配置（WECHAT/ALIPAY/UNIONPAY）。
 * 基础费率 baseRate 为平台默认；商户差异化费率见 {@link MerchantRate}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayChannel extends Entity {

    /** 渠道编码：WECHAT / ALIPAY / UNIONPAY */
    private String channelCode;

    /** 渠道名称 */
    private String channelName;

    /** 平台默认费率（如 0.006 = 千分之六，按交易额比例） */
    private BigDecimal baseRate;

    /** 渠道开关：1-开启 0-关闭 */
    private int status;

    /** 渠道参数（JSON，支付渠道商户号/appId 等，演示用） */
    private String configJson;
}