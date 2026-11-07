package com.mall.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 接口签名工具（《行业工程实践》实验指导书规范）：
 * 所有业务接口将业务参数（不含 sign）按键名升序拼接为 key1=value1&amp;key2=value2...
 * 末尾拼接渠道密钥 secretKey 后做 MD5（大写）生成 sign，服务端验签通过后方可执行业务逻辑。
 * 参数中必须携带 timestamp（秒级时间戳）与 nonce（随机串）参与签名，防止重放。
 */
public final class SignUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SignUtil() {
    }

    /**
     * 计算签名。
     * @param params 业务请求参数（可含 sign，会被排除；也可含 timestamp/nonce）
     * @param secretKey 服务端/客户端共享密钥
     */
    public static String sign(Map<String, Object> params, String secretKey) {
        // TreeMap 按键名自然升序
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (e.getValue() == null || "sign".equals(e.getKey())) {
                continue;
            }
            sorted.put(e.getKey(), e.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> sb.append(k).append('=').append(compact(v)).append('&'));
        if (!sorted.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1); // 去掉末尾 &
        }
        sb.append(secretKey); // 拼接密钥
        return md5(sb.toString()).toUpperCase();
    }

    /** 复杂值（数组/对象）统一 JSON 序列化，保证与客户端签名字节一致 */
    private static String compact(Object v) {
        if (v instanceof Map || v instanceof List) {
            try {
                return MAPPER.writeValueAsString(v);
            } catch (Exception e) {
                return String.valueOf(v);
            }
        }
        return String.valueOf(v);
    }

    /** 验签：从 body 中取 sign 与其他参数重新计算比对（不区分大小写） */
    public static boolean verify(Map<String, Object> params, String secretKey) {
        Object sign = params.get("sign");
        if (sign == null || String.valueOf(sign).isEmpty()) {
            return false;
        }
        String calc = sign(params, secretKey);
        return calc.equalsIgnoreCase(String.valueOf(sign));
    }

    /** 校验时间戳在 ±5 分钟窗内，防重放 */
    public static boolean checkTimestamp(Map<String, Object> params) {
        Object ts = params.get("timestamp");
        if (ts == null) {
            return false;
        }
        try {
            long t = Long.parseLong(String.valueOf(ts));
            return Math.abs(System.currentTimeMillis() / 1000 - t) <= 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }
}