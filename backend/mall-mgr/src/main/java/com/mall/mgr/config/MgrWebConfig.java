package com.mall.mgr.config;

import com.mall.common.filter.InternalAuthFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * mall-mgr Web 配置：注册内部接口鉴权过滤器（/internal/*）。
 */
@Configuration
public class MgrWebConfig implements WebMvcConfigurer {

    @Value("${mall.internal.token}")
    private String internalToken;

    @Bean
    public FilterRegistrationBean<Filter> internalAuthFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new InternalAuthFilter(internalToken));
        bean.addUrlPatterns("/internal/*");
        bean.setName("internalAuthFilter");
        bean.setOrder(10);
        return bean;
    }
}