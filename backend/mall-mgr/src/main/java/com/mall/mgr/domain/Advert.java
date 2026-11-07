package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 广告内容（挂载到广告位下）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Advert extends Entity {

    private Long slotId;

    private String title;

    private String imageUrl;

    /** 跳转地址（支持：product 商品ID / category 分类ID / home 首页） */
    private String linkUrl;

    private int sort;

    private String startTime;

    private String endTime;

    /** 0-下架 1-上架 */
    private int status;
}