package com.mall.mgr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.redis.RedisTools;
import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 分布式缓存（指导书第 2 章「Redis：分布式缓存」）：
 * 首页聚合与分类树属于高频只读热点数据，优先读 Redis，未命中再查库并回填；
 * 后台/商家完成写操作后统一调用 evict 失效，保证缓存与数据库最终一致。
 * Redis 不可用时 RedisTools 自动降级，读写均跳过缓存，不影响业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    /** 首页聚合（banner + 分类导航 + 推荐 + 热卖 + 猜你喜欢） */
    public static final String PRODUCT_HOME = "mall:cache:product:home";
    /** 分类树 */
    public static final String CATEGORY_TREE = "mall:cache:category:tree";

    /** 首页含商品库存展示，TTL 取 60 秒，兼顾缓存收益与库存展示时效 */
    private static final Duration HOME_TTL = Duration.ofSeconds(60);
    /** 分类树变更频率低，TTL 取 10 分钟 */
    private static final Duration TREE_TTL = Duration.ofMinutes(10);

    private final RedisTools redisTools;
    private final ObjectMapper mapper;

    /** 读缓存：未命中或反序列化失败返回 null，由调用方回源数据库 */
    public Result get(String key) {
        String json = redisTools.get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, Result.class);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，key={}：{}", key, e.getMessage());
            return null;
        }
    }

    public void putHome(Result value) {
        put(PRODUCT_HOME, value, HOME_TTL);
    }

    public void putTree(Result value) {
        put(CATEGORY_TREE, value, TREE_TTL);
    }

    private void put(String key, Result value, Duration ttl) {
        try {
            redisTools.set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("缓存写入失败，key={}：{}", key, e.getMessage());
        }
    }

    /** 商品/广告变更后失效首页缓存 */
    public void evictHome() {
        redisTools.delete(PRODUCT_HOME);
    }

    /** 分类变更后同时失效首页与分类树缓存 */
    public void evictCategoryChange() {
        redisTools.delete(PRODUCT_HOME);
        redisTools.delete(CATEGORY_TREE);
    }
}
