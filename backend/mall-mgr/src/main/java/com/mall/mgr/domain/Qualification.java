package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户资质文件：business_license 营业执照 / id_card 法人身份证 / bank 银行账户
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Qualification extends Entity {

    private Long merchantId;

    /** 商户编号 */
    private String merchantNo;

    /** 资质类型：business_license / id_card / bank */
    private String type;

    /** 资质名称（如：XX 公司营业执照） */
    private String name;

    /** 文件地址（演示用模拟地址，真实环境接 OSS） */
    private String url;

    /** 审核状态：0-待审 1-通过 2-驳回 */
    private int auditStatus;

    /** 审核意见 */
    private String auditRemark;
}