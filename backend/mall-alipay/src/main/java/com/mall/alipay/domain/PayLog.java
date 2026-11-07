package com.mall.alipay.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付流水日志（下单/回调/关单等事件，供对账与排查）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayLog extends Entity {

    private String payOrderId;

    private String orderNo;

    private String channelCode;

    /** 事件：create / callback / close / refund */
    private String event;

    private String remark;
}