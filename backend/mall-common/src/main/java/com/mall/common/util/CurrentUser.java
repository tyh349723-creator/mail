package com.mall.common.util;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前请求身份获取：网关校验 JWT 后注入 X-User-* 头，业务服务从请求头读取。
 * 游客（未登录）可访问公开接口，此时 userId/null。
 */
public final class CurrentUser {

    public static final String HDR_USER_ID = "X-User-Id";
    public static final String HDR_USER_TYPE = "X-User-Type";
    public static final String HDR_USER_NUM = "X-User-Num";

    private CurrentUser() {
    }

    public static String header(String name) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader(name);
    }

    /** 用户/商户/管理员主键 */
    public static Long userId() {
        String v = header(HDR_USER_ID);
        return StringUtils.hasText(v) ? Long.parseLong(v) : null;
    }

    /** user / merchant / admin */
    public static String userType() {
        return header(HDR_USER_TYPE);
    }

    /** userNum / shopNum */
    public static String userNum() {
        return header(HDR_USER_NUM);
    }

    public static boolean isLogin() {
        return userId() != null;
    }

    public static boolean isMerchant() {
        return JwtUtil.TYPE_MERCHANT.equals(userType());
    }

    public static boolean isAdmin() {
        return JwtUtil.TYPE_ADMIN.equals(userType());
    }

    public static boolean isUser() {
        return JwtUtil.TYPE_USER.equals(userType());
    }
}