package com.mall.mgr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入驻商户（店铺）。状态：0-待审核 1-正常 2-已驳回 3-已禁用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends Entity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户唯一编号 shopNum */
    @TableField("merchant_no")
    private String merchantNo;

    /** 登录账号 */
    private String username;

    /** 登录密码（明文演示，生产应 BCrypt 加盐） */
    private String password;

    /** 店铺名称 */
    @TableField("shop_name")
    private String shopName;

    /** 联系手机号 */
    @TableField("shop_phone")
    private String shopPhone;

    /** 经营地址 */
    @TableField("shop_address")
    private String shopAddress;

    /** 联系人姓名 */
    @TableField("contact_name")
    private String contactName;

    /** 经营类目名称 */
    @TableField("category_name")
    private String categoryName;

    /** 状态：0-待审核 1-正常 2-已驳回 3-已禁用 */
    private int status;

    /** 审核备注（驳回原因等） */
    @TableField("reject_reason")
    private String rejectReason;

    /** 审核操作管理员名 */
    private String auditor;

    /** 审核时间 */
    @TableField("audit_time")
    private String auditTime;
}