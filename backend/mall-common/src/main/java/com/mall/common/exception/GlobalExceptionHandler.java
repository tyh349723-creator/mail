package com.mall.common.exception;

import com.mall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常返回友好提示，其余记录日志后统一返回“系统繁忙”。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Result handleApi(ApiException e) {
        return Result.err(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.err("1", msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleReadable(HttpMessageNotReadableException e) {
        return Result.err("1", "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.err(Result.CODE_SERVER_ERROR, "系统繁忙，请稍后再试");
    }
}