package com.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一网关 mall-gateway（端口 10010）：全部外部接口入口。
 * <p>职责：路由（/mch /user /cart /order /product /admin /statis/platform → mall-mgr；
 * /pay /refund /wallet /statis/mch → mall-alipay）、全局 MD5 验签（timestamp/nonce 防重放）、
 * Redis+Lua 固定窗口限流（降级内存）、JWT 鉴权并把身份注入 X-User-* 头、CORS。</p>
 */
@SpringBootApplication(scanBasePackages = "com.mall")
public class MallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGatewayApplication.class, args);
    }
}