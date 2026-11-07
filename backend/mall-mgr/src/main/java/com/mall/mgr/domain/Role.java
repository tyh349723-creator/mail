package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员角色（RBAC）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends Entity {

    private String roleName;

    private String roleCode;

    /** 角色描述 */
    private String description;

    /** 0-停用 1-启用 */
    private int status;

    /** 权限码集合（逗号分隔），如 merchant:review,order:list,report:view */
    private String perms;
}