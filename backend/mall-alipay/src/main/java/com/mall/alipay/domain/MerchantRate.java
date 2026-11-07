package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商户差异化费率（覆盖渠道基础费率）。键：(merchantNo, wayCode)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantRate extends Entity {

    /** 商户编号 */
    private String merchantNo;

    /** 渠道编码 */
    private String wayCode;

    /** 给该商户的费率（如 0.003） */
    private BigDecimal mchRate;
}