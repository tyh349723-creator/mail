package com.mall.mgr.config;

import com.mall.mgr.service.MonitorService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 接口调用监控过滤器：统计业务接口调用次数（排除内部通道 /internal、错误页 /error），
 * 数据交由 {@link MonitorService} 采集，管理端「接口监控」页展示。
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class ApiMonitorFilter implements Filter {

    private final MonitorService monitorService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request instanceof HttpServletRequest req ? req.getRequestURI() : "";
        if (!path.startsWith("/internal") && !path.startsWith("/error") && !"/".equals(path)) {
            monitorService.record(path);
        }
        chain.doFilter(request, response);
    }
}