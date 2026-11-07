package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商户提现申请。状态：0 待审核 1 已打款 2 已驳回。
 * 提现申请时从钱包余额扣除，驳回时退回。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Withdraw extends Entity {

    /** 提现单号 */
    private String withdrawNo;

    /** 商户编号 */
    private String merchantNo;

    /** 提现金额（元） */
    private BigDecimal money;

    /** 收款账户 */
    private String bankName;

    /** 收款卡号 */
    private String cardNo;

    /** 状态：0 待审核 1 已打款 2 已驳回 */
    private int status;

    /** 审核时间 */
    private String auditTime;

    /** 审核意见 */
    private String auditRemark;
}