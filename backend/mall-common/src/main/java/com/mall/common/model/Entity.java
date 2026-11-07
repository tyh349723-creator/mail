package com.mall.common.model;

import lombok.Data;

/**
 * 内存实体基类：主键 + 创建/更新时间。
 * 约定：后续接入 MySQL 时，实体通过注解映射表结构（预留替换点）。
 */
@Data
public abstract class Entity {

    /** 主键（内存自增 / 数据库自增） */
    private Long id;

    /** 创建时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;

    /** 更新时间 yyyy-MM-dd HH:mm:ss */
    private String updateTime;
}