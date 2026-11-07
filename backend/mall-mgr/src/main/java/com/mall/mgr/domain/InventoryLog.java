package com.mall.mgr.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存流水：记录每次 SKU 库存变动（下单扣减 / 关单释放），用于对账与排查库存对不上。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_log")
public class InventoryLog extends Entity {

    /** SKU ID */
    @TableField("sku_id")
    private Long skuId;

    /** 商品ID */
    @TableField("product_id")
    private Long productId;

    /** 关联业务订单号 */
    @TableField("order_no")
    private String orderNo;

    /** 变动类型：下单扣减 / 超时未支付 / 用户取消 / 平台关闭 */
    @TableField("change_type")
    private String changeType;

    /** 变动数量（正数入库，负数出库） */
    @TableField("change_qty")
    private Integer changeQty;

    /** 变动前 SKU 库存 */
    @TableField("before_stock")
    private Integer beforeStock;

    /** 变动后 SKU 库存 */
    @TableField("after_stock")
    private Integer afterStock;

    /** 备注 */
    private String remark;
}
