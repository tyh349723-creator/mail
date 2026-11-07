package com.mall.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 轻量 JWT（HMAC-SHA256），无第三方依赖。
 * <p>签发/校验关键链路：商户端登录(mgr签发) → 网关校验并注入用户头 → 业务服务按头拿身份。
 * 载荷字段：uid(业务主键)、type(user/merchant/admin)、num(业务编号: userNum/shopNum/无)、sub(用户名)、exp(秒)。</p>
 */
public final class JwtUtil {

    private static final String HEADER = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 用户类型 */
    public static final String TYPE_USER = "user";
    public static final String TYPE_MERCHANT = "merchant";
    public static final String TYPE_ADMIN = "admin";

    private JwtUtil() {
    }

    public static String create(Map<String, Object> claims, String secret, long expireSeconds) {
        Map<String, Object> payload = new HashMap<>(claims);
        payload.put("exp", System.currentTimeMillis() / 1000 + expireSeconds);
        try {
            String p = b64(MAPPER.writeValueAsString(payload));
            String signingInput = HEADER + "." + p;
            String sig = b64(hmac(signingInput, secret));
            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("令牌签发失败", e);
        }
    }

    /**
     * 解析并校验令牌。
     * @return 载荷 claims；签名错误或过期返回 null
     */
    public static Map<String, Object> parse(String token, String secret) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = b64(hmac(signingInput, secret));
        // 常量时间比较，防时序攻击
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            Map<String, Object> claims = MAPPER.readValue(dec(parts[1]), Map.class);
            Object exp = claims.get("exp");
            if (exp == null || Long.parseLong(String.valueOf(exp)) * 1000 < System.currentTimeMillis()) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String dec(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
}