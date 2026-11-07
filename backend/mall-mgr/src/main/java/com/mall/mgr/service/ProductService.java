package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.mgr.domain.Advert;
import com.mall.mgr.domain.Category;
import com.mall.mgr.domain.Product;
import com.mall.mgr.domain.ProductSku;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 商品服务：首页聚合（轮播/分类/推荐/热卖）、分类树、商品列表搜索、商品详情。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final AdvertMapper advertMapper;
    private final CacheService cacheService;

    /** 首页聚合数据：banner + 分类金刚区 + 推荐 + 热卖 + 猜你喜欢（优先读 Redis 缓存） */
    public Result home() {
        Result cached = cacheService.get(CacheService.PRODUCT_HOME);
        if (cached != null) {
            return cached;
        }
        Result r = Result.ok();
        // 轮播广告（启用状态 + 当前时间段内）
        List<Map<String, Object>> banners = new ArrayList<>();
        for (Advert a : advertMapper.selectList(new QueryWrapper<Advert>().eq("status", 1))) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("id", a.getId());
            b.put("title", a.getTitle());
            b.put("imageUrl", a.getImageUrl());
            b.put("linkUrl", a.getLinkUrl());
            banners.add(b);
        }
        r.add("banners", banners);
        // 一级分类导航
        List<Map<String, Object>> categoryNav = new ArrayList<>();
        for (Category c : categoryMapper.selectList(new QueryWrapper<Category>().eq("status", 1).eq("level", 1))) {
            categoryNav.add(catMap(c));
        }
        r.add("categories", categoryNav);
        // 推荐位商品
        r.add("recommend", productMapper.selectList(new QueryWrapper<Product>()
                .eq("status", StatusConst.PRODUCT_ON).eq("is_recommend", 1)));
        // 热卖榜（已售降序前 8）
        List<Product> hot = productMapper.selectList(new QueryWrapper<Product>().eq("status", StatusConst.PRODUCT_ON));
        hot.sort(Comparator.comparingInt(Product::getSales).reversed());
        r.add("hot", briefList(hot.stream().limit(8).toList()));
        // 猜你喜欢（随机推荐流，取前 10）
        List<Product> recommend = productMapper.selectList(new QueryWrapper<Product>().eq("status", StatusConst.PRODUCT_ON));
        Collections.shuffle(recommend, new Random(42));
        r.add("guessYouLike", briefList(recommend.stream().limit(10).toList()));
        cacheService.putHome(r);
        return r;
    }

    /** 商品列表/搜索：keyword / categoryId(含子级) / merchantNo / sort / 分页 */
    public Result list(Map<String, Object> body) {
        String keyword = str(body, "keyword");
        long categoryId = longv(body, "categoryId");
        String merchantNo = str(body, "merchantNo");
        String sort = str(body, "sort").isBlank() ? "default" : str(body, "sort");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);

        Set<Long> catIds = new HashSet<>();
        if (categoryId > 0) {
            catIds.add(categoryId);
            for (Category c : categoryMapper.selectList(new QueryWrapper<Category>().eq("parent_id", categoryId).eq("status", 1))) {
                catIds.add(c.getId());
            }
        }
        QueryWrapper<Product> wrapper = new QueryWrapper<Product>()
                .eq("status", StatusConst.PRODUCT_ON)
                .and(!keyword.isBlank(), w -> w.like("title", keyword).or().like("sub_title", keyword))
                .in(!catIds.isEmpty(), "category_id", catIds)
                .eq(!merchantNo.isBlank(), "merchant_no", merchantNo);
        switch (sort) {
            case "sales" -> wrapper.orderByDesc("sales");
            case "price_asc" -> wrapper.orderByAsc("price");
            case "price_desc" -> wrapper.orderByDesc("price");
            case "new" -> wrapper.orderByDesc("create_time");
            default -> wrapper.orderByDesc("sales");
        }
        long total = productMapper.selectCount(wrapper);
        List<Product> list = productMapper.selectList(wrapper.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = briefList(list);
        return Result.ok().page(total, current, size, records);
    }

    /** 商品详情：基础信息 + SKU + 店铺信息 */
    public Result detail(long id) {
        Product p = productMapper.selectOne(new QueryWrapper<Product>()
                .eq("id", id).eq("status", StatusConst.PRODUCT_ON));
        if (p == null) {
            throw new ApiException("商品不存在或已下架");
        }
        Result r = Result.ok();
        r.add("productId", p.getId());
        r.add("productNo", p.getProductNo());
        r.add("title", p.getTitle());
        r.add("subTitle", p.getSubTitle());
        r.add("mainImage", p.getMainImage());
        r.add("images", jsonList(p.getImages()));
        r.add("price", p.getPrice());
        r.add("originalPrice", p.getOriginalPrice());
        r.add("sales", p.getSales());
        r.add("detail", p.getDetail());
        r.add("categoryId", p.getCategoryId());
        Category cat = p.getCategoryId() == null ? null : categoryMapper.selectById(p.getCategoryId());
        r.add("categoryName", cat == null ? "" : cat.getName());
        r.add("merchantNo", p.getMerchantNo());
        r.add("shopName", p.getShopName());
        // SKU 列表
        List<Map<String, Object>> skuList = new ArrayList<>();
        for (ProductSku s : productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))) {
            Map<String, Object> skuMap = new LinkedHashMap<>();
            skuMap.put("id", s.getId());
            skuMap.put("specName", s.getSpecName());
            skuMap.put("specValue", s.getSpecValue());
            skuMap.put("price", s.getPrice());
            skuMap.put("stock", s.getStock());
            skuMap.put("image", s.getImage());
            skuList.add(skuMap);
        }
        r.add("skus", skuList);
        int totalStock = skuList.stream().mapToInt(s -> (Integer) s.get("stock")).sum();
        r.add("stock", totalStock);
        return r;
    }

    /** 分类树（优先读 Redis 缓存） */
    public Result categoryTree() {
        Result cached = cacheService.get(CacheService.CATEGORY_TREE);
        if (cached != null) {
            return cached;
        }
        Result r = Result.ok().add("tree", buildTree(0L));
        cacheService.putTree(r);
        return r;
    }

    private List<Map<String, Object>> buildTree(Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category c : categoryMapper.selectList(new QueryWrapper<Category>().eq("status", 1).eq("parent_id", parentId))) {
            Map<String, Object> map = catMap(c);
            List<Map<String, Object>> children = buildTree(c.getId());
            if (!children.isEmpty()) {
                map.put("children", children);
            }
            result.add(map);
        }
        return result;
    }

    // ---------- 工具 ----------

    private Map<String, Object> catMap(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("parentId", c.getParentId());
        m.put("level", c.getLevel());
        m.put("sort", c.getSort());
        m.put("icon", c.getIcon());
        return m;
    }

    private List<Map<String, Object>> briefList(List<Product> list) {
        return list.stream().map(this::brief).toList();
    }

    public Map<String, Object> brief(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("productNo", p.getProductNo());
        m.put("title", p.getTitle());
        m.put("subTitle", p.getSubTitle());
        m.put("mainImage", p.getMainImage());
        m.put("price", p.getPrice());
        m.put("originalPrice", p.getOriginalPrice());
        m.put("sales", p.getSales());
        m.put("stock", p.getStock());
        m.put("merchantNo", p.getMerchantNo());
        m.put("shopName", p.getShopName());
        m.put("isRecommend", p.getIsRecommend());
        return m;
    }

    private List<String> jsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json.replaceAll("[\\[\\]\"]", ""));
        }
    }

    private String safeSub(Product p) {
        return p.getSubTitle() == null ? "" : p.getSubTitle();
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    private int intv(Map<String, Object> body, String key, int def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private long longv(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return 0;
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}