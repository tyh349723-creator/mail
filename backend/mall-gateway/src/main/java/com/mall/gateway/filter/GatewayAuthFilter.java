package com.mall.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.redis.RedisTools;
import com.mall.common.result.Result;
import com.mall.common.util.JwtUtil;
import com.mall.common.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 网关统一过滤器（顺序 0）：
 * ① 读取并缓存请求体（POST + JSON）；② MD5 验签（timestamp/nonce 防重放，登录注册同样验签）；
 * ③ Redis+Lua 固定窗口限流（Redis 不可用降级内存）；④ JWT 鉴权并把 X-User-* 身份头注入下游；
 * ⑤ 将请求体原样转发给下游微服务。
 * <p>免验签白名单：仅模拟渠道回调 /pay/local-callback/**（浏览器同源触发，生产由渠道服务器验签回调 /pay/callback）。
 * 免登录白名单：登录注册 / 游客浏览 / 模拟收银台页面 / 渠道回调。</p>
 */
@Slf4j
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    /** 免登录前缀（无需 JWT；游客可访问，例如游客浏览商品） */
    private static final Set<String> JWT_FREE = Set.of(
            "/user/login", "/user/register", "/mch/login", "/mch/register", "/admin/login",
            "/product/", "/pay/channels", "/pay/cashier/", "/pay/result/",
            "/pay/local-callback/", "/pay/callback", "/refund/callback", "/pay/alipay/");

    /** 免验签白名单（模拟渠道回调与支付宝回调：前者浏览器发起拿不到密钥，后者由支付宝 RSA2 验签） */
    private static final Set<String> SIGN_FREE_PREFIX = Set.of(
            "/pay/local-callback/", "/pay/alipay/notify", "/pay/alipay/return");

    private final ObjectMapper mapper = new ObjectMapper();
    private final RedisTools redisTools;

    @Value("${mall.token.secret}")
    private String tokenSecret;

    /** 全局请求验签密钥：由 MALL_SIGN_SECRET 环境变量注入，与前端 VITE_SIGN_SECRET 必须一致 */
    @Value("${mall.sign.secret}")
    private String signSecret;

    public GatewayAuthFilter(RedisTools redisTools) {
        this.redisTools = redisTools;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() == null ? "GET" : request.getMethod().name();

        if ("GET".equalsIgnoreCase(method)) {
            if (!isJwtFree(path)) {
                return writeJson(exchange, Result.err("401", "请先登录"));
            }
            return chain.filter(exchange);
        }
        // POST：读取请求体 → 验签 → 限流 → 鉴权 → 转发
        return DataBufferUtils.join(request.getBody()).flatMap(buf -> {
            byte[] bytes = new byte[buf.readableByteCount()];
            buf.read(bytes);
            DataBufferUtils.release(buf);
            Map<String, Object> json = parse(new String(bytes, StandardCharsets.UTF_8));

            // ① 验签
            if (!isSignFree(path)
                    && (!SignUtil.checkTimestamp(json) || !SignUtil.verify(json, signSecret))) {
                return writeJson(exchange, Result.err("403", "签名校验失败"));
            }
            // ② 限流：固定窗口 Redis Lua（key = 路径 + 身份/匿名IP）
            String rateKey = path + "|" + (StringUtils.hasText(header(request, "X-User-Num"))
                    ? header(request, "X-User-Num") : remoteIp(request));
            if (!redisTools.rateLimit(rateKey, limitOf(path), 60)) {
                return writeJson(exchange, Result.limit());
            }
            // ③ JWT 鉴权 + 身份注入
            Map<String, Object> claims = null;
            String token = bearer(request);
            if (StringUtils.hasText(token)) {
                claims = JwtUtil.parse(token, tokenSecret);
                if (claims == null && !isJwtFree(path)) {
                    return writeJson(exchange, Result.err("401", "登录状态已失效，请重新登录"));
                }
            }
            if (claims == null && !isJwtFree(path)) {
                return writeJson(exchange, Result.err("401", "请先登录"));
            }
            Map<String, String> userHeaders = userHeaders(claims);
            // ④ 重建请求体并转发
            ServerHttpRequest mutated = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    // 源码请求头为 ReadOnlyHttpHeaders，必须生成可写副本（writableHttpHeaders）才不会抛 UnsupportedOperationException
                    HttpHeaders headers = HttpHeaders.writableHttpHeaders(super.getHeaders());
                    headers.setContentLength(bytes.length);
                    userHeaders.forEach(headers::set);
                    return headers;
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return Flux.just(buffer);
                }
            };
            return chain.filter(exchange.mutate().request(mutated).build());
        });
    }

    private Map<String, String> userHeaders(Map<String, Object> claims) {
        Map<String, String> h = new HashMap<>();
        if (claims != null) {
            h.put("X-User-Id", String.valueOf(claims.getOrDefault("uid", "")));
            h.put("X-User-Type", String.valueOf(claims.getOrDefault("type", "")));
            h.put("X-User-Num", String.valueOf(claims.getOrDefault("num", "")));
        }
        return h;
    }

    /** 不同路径限流阈值：交易类接口严格，浏览类宽松 */
    private long limitOf(String path) {
        if (path.startsWith("/pay/callback") || path.startsWith("/pay/local-callback")) {
            return 20;
        }
        // 支付结果轮询（只读）：前端约每 2 秒查询一次，阈值需高于默认值，避免轮询被误限流
        if (path.startsWith("/pay/query")) {
            return 120;
        }
        if (path.startsWith("/product/") || path.startsWith("/pay/channels")) {
            return 60;
        }
        return 30;
    }

    private boolean isJwtFree(String path) {
        return JWT_FREE.stream().anyMatch(path::startsWith);
    }

    private boolean isSignFree(String path) {
        return SIGN_FREE_PREFIX.stream().anyMatch(path::startsWith);
    }

    private Map<String, Object> parse(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String bearer(ServerHttpRequest request) {
        String auth = header(request, "Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }

    private String header(ServerHttpRequest request, String name) {
        List<String> values = request.getHeaders().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String remoteIp(ServerHttpRequest request) {
        String xff = header(request, "X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddress() == null ? "unknown" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, Result result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = mapper.writeValueAsBytes(result);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Flux.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}