package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台管理员账号
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUser extends Entity {

    private String username;

    private String password;

    private String nickname;

    /** 角色编码（逗号分隔，如 super_admin,operator） */
    private String roles;

    /** 0-停用 1-启用 */
    private int status;
}