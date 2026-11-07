package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收货地址
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Address extends Entity {

    private Long userId;

    /** 收货人 */
    private String consignee;

    /** 联系电话 */
    private String phone;

    private String province;

    private String city;

    private String district;

    /** 详细地址 */
    private String detail;

    /** 是否默认地址 */
    private boolean defaultFlag;
}