package com.mall.mgr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类（平台维护）。两级：顶级 level=1，子级 level=2。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends Entity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 0-顶级，否则父分类 id */
    @TableField("parent_id")
    private Long parentId;

    /** 分类层级 1/2 */
    private int level;

    private int sort;

    private String icon;

    /** 0-停用 1-启用 */
    private int status;
}