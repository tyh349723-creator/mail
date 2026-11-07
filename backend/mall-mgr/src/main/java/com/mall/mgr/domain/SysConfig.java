package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台系统参数（5.3.7 系统运维-系统参数配置）。
 * 名值对存储：configKey 全局唯一，configValue 为配置值（字符串，数字/文案均可）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfig extends Entity {

    /** 配置键（唯一），如 order.timeout / refund.large-threshold */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String remark;
}