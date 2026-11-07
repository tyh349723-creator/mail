package com.mall.common.filter;

import com.mall.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 内部服务互调鉴权过滤器：仅校验 X-Internal-Token 请求头（服务间直连，避免外部伪造 /internal/** 接口）。
 * 由各 Servlet 微服务（mall-mgr / mall-alipay）显式注册到 /internal/* 路径。
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    private final String internalToken;
    private final ObjectMapper mapper = new ObjectMapper();

    public InternalAuthFilter(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("X-Internal-Token");
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(token)) {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(mapper.writeValueAsBytes(
                    Result.err("403", "内部接口鉴权失败")));
            return;
        }
        chain.doFilter(request, response);
    }
}