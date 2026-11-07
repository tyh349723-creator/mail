package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 广告位：position 标识首页轮播/分类页/详情页
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdvertSlot extends Entity {

    private String slotName;

    /** 编码：home_banner / category_banner / detail_banner */
    private String slotCode;

    private String position;

    /** 0-停用 1-启用 */
    private int status;
}