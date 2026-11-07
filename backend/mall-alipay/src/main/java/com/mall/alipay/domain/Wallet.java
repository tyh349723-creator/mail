package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商户钱包：交易实收（扣除手续费后入账）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Wallet extends Entity {

    /** 商户编号（唯一） */
    private String merchantNo;

    /** 可提现余额（元） */
    private BigDecimal balance;

    /** 累计入账净额（元） */
    private BigDecimal totalIncome;

    /** 累计手续费（元） */
    private BigDecimal totalFee;
}