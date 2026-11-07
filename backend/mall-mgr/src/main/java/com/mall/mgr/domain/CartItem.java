package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 购物车条目（服务端，按用户维度；游客购物车由移动端本地离线缓存，登录后合并到本表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CartItem extends Entity {

    private Long userId;

    private Long productId;

    private Long skuId;

    /** 商品标题快照 */
    private String productTitle;

    /** 商品图片快照 */
    private String productImage;

    /** 规格信息，如：颜色:红色 */
    private String specInfo;

    /** 单价（元） */
    private BigDecimal price;

    private int quantity;

    /** 勾选状态（结算时只结算勾选项） */
    private boolean checked;
}