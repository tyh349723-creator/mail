package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单商品快照（下单时固化价格与名称，后续改价不影响历史订单）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItem extends Entity {

    /** 所属订单ID */
    private Long orderId;

    private Long productId;

    private Long skuId;

    private String productTitle;

    private String productImage;

    /** 规格信息 */
    private String specInfo;

    /** 成交单价（元） */
    private BigDecimal price;

    private int quantity;
}