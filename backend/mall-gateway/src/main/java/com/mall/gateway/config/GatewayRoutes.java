package com.mall.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 静态路由：默认指向本机 10011/10012。
 * Docker 部署时通过配置 mall.routes.* 切换为容器服务名（mall-mgr / mall-alipay）。
 */
@Configuration
public class GatewayRoutes {

    @Value("${mall.routes.mgr-uri:http://localhost:10011}")
    private String mgrUri;

    @Value("${mall.routes.alipay-uri:http://localhost:10012}")
    private String alipayUri;

    @Bean
    public RouteLocator mallRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // 业务微服务：商户/用户/购物车/订单/商品/管理端/平台统计
                .route("mgr", r -> r.path("/mch/**", "/user/**", "/cart/**", "/order/**",
                                "/product/**", "/admin/**", "/internal/**", "/statis/platform")
                        .uri(mgrUri))
                // 支付微服务：支付/退款/钱包/商户统计/模拟收银台
                .route("alipay", r -> r.path("/pay/**", "/refund/**", "/wallet/**", "/statis/mch")
                        .uri(alipayUri))
                .build();
    }
}