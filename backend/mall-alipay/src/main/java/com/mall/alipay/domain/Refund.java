package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 退款单。状态：0 审核中 1 退款成功 2 退款失败（驳回）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Refund extends Entity {

    /** 退款单号 */
    private String refundOrderId;

    /** 原业务订单号 */
    private String orderNo;

    /** 商户编号 */
    private String merchantNo;

    /** 退款金额（元） */
    private BigDecimal money;

    /** 退款原因 */
    private String reason;

    /** 状态：0 审核中 1 成功 2 失败 */
    private int status;

    /** 审核意见 */
    private String auditRemark;

    /** 审核时间 */
    private String auditTime;
}