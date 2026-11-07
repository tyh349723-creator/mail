package com.mall.common.exception;

import lombok.Getter;

/**
 * 业务异常：message 直接作为响应提示文案，code 作为响应 id。
 */
@Getter
public class ApiException extends RuntimeException {

    private final String code;

    public ApiException(String message) {
        this("1", message);
    }

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** 未登录/登录失效 */
    public static ApiException unauthorized() {
        return new ApiException("401", "未登录或登录已失效");
    }

    /** 签名校验失败 */
    public static ApiException signError() {
        return new ApiException("403", "签名校验失败");
    }
}