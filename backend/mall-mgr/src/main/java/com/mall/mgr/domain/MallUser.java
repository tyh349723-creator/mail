package com.mall.mgr.domain;

import com.mall.common.model.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端（普通用户）账号
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MallUser extends Entity {

    /** 用户唯一编号 userNum（下单/支付侧标识） */
    private String userNum;

    /** 登录账号 */
    private String username;

    /** 登录密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 账号状态：1 正常 / 0 禁用 */
    private Integer status;
}