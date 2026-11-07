package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SKU 规格（至少一个「默认」SKU，库存按 SKU 独立维护）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductSku extends Entity {

    private Long productId;

    /** 规格名，如：颜色 / 包装 / 默认 */
    private String specName;

    /** 规格值，如：红色 / 单件装 / 默认规格 */
    private String specValue;

    /** SKU 价格（元） */
    private BigDecimal price;

    /** SKU 库存 */
    private int stock;

    /** SKU 图片（可空，为空沿用商品主图） */
    private String image;
}