package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包流水。tradeType：pay_in 交易入账 / refund_out 退款支出 / withdraw_out 提现支出 / withdraw_back 提现驳回退回。
 * <p>pay_in 记录 money=交易额（营收），fee=渠道手续费，实收 = money - fee；其余类型 fee 为空（填 0）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WalletRecord extends Entity {

    private String merchantNo;

    private String tradeType;

    /** 变动金额（pay_in 为交易额） */
    private BigDecimal money;

    /** 手续费（仅 pay_in 有值） */
    private BigDecimal fee = BigDecimal.ZERO;

    private String remark;
}