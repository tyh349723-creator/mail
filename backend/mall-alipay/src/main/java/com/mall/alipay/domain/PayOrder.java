package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付单（对应一笔业务订单的一次支付）。
 * 状态机：-1 待支付 → 0 支付成功 / 1 支付失败 / 3 已关闭；支付成功后 → 2 已退款。
 * 支付回调幂等：Redis 锁 + payOrderId 唯一 + 状态机三重保障。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayOrder extends Entity {

    /** 支付单号 payOrderId（对外唯一标识） */
    private String payOrderId;

    /** 业务订单号（商户侧订单） */
    private String orderNo;

    /** 商户编号 */
    private String merchantNo;

    /** 支付渠道编码 */
    private String channelCode;

    /** 交易金额（元） */
    private BigDecimal money;

    /** 手续费（元） */
    private BigDecimal fee;

    /** 状态：-1 待支付 0 成功 1 失败 2 已退款 3 已关闭 */
    private int status;

    /** 支付时间（yyyy-MM-dd HH:mm:ss） */
    private String payTime;

    /** 关闭时间（超时未支付 / 取消 / 平台关闭） */
    private String closeTime;

    /** 关闭原因：超时未支付 / 用户取消 / 平台关闭 */
    private String closeReason;
}