package com.mall.mgr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品。状态：0-下架 1-上架 2-删除
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends Entity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品编号 productNo */
    @TableField("product_no")
    private String productNo;

    @TableField("merchant_id")
    private Long merchantId;

    /** 所属商户编号 */
    @TableField("merchant_no")
    private String merchantNo;

    /** 店铺名称快照 */
    @TableField("shop_name")
    private String shopName;

    /** 商品标题 */
    private String title;

    /** 副标题/卖点 */
    @TableField("sub_title")
    private String subTitle;

    @TableField("category_id")
    private Long categoryId;

    /** 主图地址 */
    @TableField("main_image")
    private String mainImage;

    /** 轮播图片（JSON 数组） */
    private String images;

    /** 现价（元） */
    private BigDecimal price;

    /** 原价（元） */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /** 库存（内存聚合值，扣减走 Redis Lua 原子操作） */
    private int stock;

    /** 已售 */
    private int sales;

    /** 状态：0-下架 1-上架 2-删除 */
    private int status;

    /** 是否推荐（首页推荐位）：0-否 1-是 */
    @TableField("is_recommend")
    private int isRecommend;

    /** 详情图文（HTML） */
    private String detail;

    /** SKU 列表（至少一个默认 SKU） */
    @TableField(exist = false)
    private List<ProductSku> skus = new ArrayList<>();
}