package com.mall.common.result;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 统一响应结构（《行业工程实践》实验指导书规范）：
 * 响应体固定为 {id, message, ...业务字段}，id 为 "0" 表示成功，非 0 失败并携带 message 提示。
 * 兼容 Jackson 序列化：本质是 LinkedHashMap，业务字段直接平铺。
 */
public class Result extends LinkedHashMap<String, Object> {

    /** 成功码 */
    public static final String CODE_OK = "0";
    /** 通用业务失败码 */
    public static final String CODE_ERROR = "1";
    /** 未登录 / 登录态失效 */
    public static final String CODE_UNAUTHORIZED = "401";
    /** 签名校验失败 */
    public static final String CODE_SIGN_ERROR = "403";
    /** 限流拦截 */
    public static final String CODE_LIMIT = "429";
    /** 服务器内部错误 */
    public static final String CODE_SERVER_ERROR = "500";

    public static Result ok() {
        return ok("成功");
    }

    public static Result ok(String message) {
        Result r = new Result();
        r.put("id", CODE_OK);
        r.put("message", message);
        return r;
    }

    public static Result err(String message) {
        return err(CODE_ERROR, message);
    }

    public static Result err(String code, String message) {
        Result r = new Result();
        r.put("id", code);
        r.put("message", message);
        return r;
    }

    /** 限流拦截统一返回（指导书：{"message":"系统繁忙，请稍后再试"}） */
    public static Result limit() {
        return err(CODE_LIMIT, "系统繁忙，请稍后再试");
    }

    /**
     * 添加业务字段。
     * @return 当前 Result，支持链式调用
     */
    public Result add(String key, Object value) {
        this.put(key, value);
        return this;
    }

    /**
     * 分页数据（字段对齐指导书 /pay/list/mch 返回：total/pages/current/size/records）。
     */
    public Result page(long total, long current, long size, List<?> records) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        this.put("total", total);
        this.put("pages", pages);
        this.put("current", current);
        this.put("size", size);
        this.put("records", records);
        return this;
    }

    public boolean isSuccess() {
        return CODE_OK.equals(this.get("id"));
    }
}