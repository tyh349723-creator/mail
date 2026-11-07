package com.mall.mgr.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务订单（用户/商户视角的交易单）。
 * <p>状态（指导书 0/1/2/3 + 扩展）：-1 待支付，0 已支付，1 支付失败，2 已退款，3 订单关闭，4 已发货，5 已完成</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends Entity {

    /** 业务订单号 mchOrderNo（商户侧订单号，作为支付侧 orderNum） */
    private String orderNo;

    private Long userId;

    /** 下单用户编号 */
    private String userNum;

    /** 买家昵称快照 */
    private String buyerName;

    private Long merchantId;

    /** 商户编号 shopNum */
    private String merchantNo;

    /** 店铺名称快照 */
    private String shopName;

    /** 商品总额（元） */
    private BigDecimal totalAmount;

    /** 应付金额（元，= 总额 - 优惠，本项目无优惠，与总额一致） */
    private BigDecimal payAmount;

    /** 状态：-1 待支付 0 已支付 1 支付失败 2 已退款 3 订单关闭 4 已发货 5 已完成 */
    private int status;

    /** 支付渠道编码：WECHAT/ALIPAY/UNIONPAY */
    private String channel;

    /** 支付时间 */
    private String payTime;

    /** 收货人 */
    private String consignee;

    /** 联系电话 */
    private String phone;

    /** 收货地址（省市区+详细） */
    private String address;

    /** 买家备注 */
    private String remark;

    /** 关闭时间（超时未支付 / 取消） */
    private String closeTime;

    /** 关闭原因：超时未支付 / 用户取消 / 平台关闭 */
    private String closeReason;

    /** 支付超时时长（分钟），下单时固化；为空则用全局配置 */
    private Integer payTimeoutMinutes;

    /** 确认收货时间 */
    private String finishTime;

    /** 运费（元，本项目包邮） */
    private BigDecimal freight = BigDecimal.ZERO;

    /** 订单商品快照（不落库，由 order_item 表关联） */
    @TableField(exist = false)
    private List<OrderItem> items = new ArrayList<>();
}