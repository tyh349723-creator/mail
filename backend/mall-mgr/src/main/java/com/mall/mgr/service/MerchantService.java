package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.common.util.NoGen;
import com.mall.mgr.domain.*;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户服务：入驻资料提交/审核状态/店铺信息维护/商品管理。
 */
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantMapper merchantMapper;
    private final QualificationMapper qualificationMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    private Merchant currentMerchant() {
        if (!CurrentUser.isMerchant()) {
            throw ApiException.unauthorized();
        }
        String shopNum = CurrentUser.userNum();
        Merchant m = merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("merchant_no", shopNum));
        if (m == null) {
            throw ApiException.unauthorized();
        }
        return m;
    }

    /** 提交/完善入驻资料（资质多图），此时状态回到待审核 */
    public Result apply(Map<String, Object> body) {
        Merchant m = currentMerchant();
        m.setShopName(str(body, "shopName").isBlank() ? m.getShopName() : str(body, "shopName"));
        m.setShopPhone(str(body, "shopPhone").isBlank() ? m.getShopPhone() : str(body, "shopPhone"));
        m.setShopAddress(str(body, "shopAddress").isBlank() ? m.getShopAddress() : str(body, "shopAddress"));
        m.setContactName(str(body, "contactName").isBlank() ? m.getContactName() : str(body, "contactName"));
        m.setCategoryName(str(body, "categoryName"));
        m.setStatus(StatusConst.MCH_PENDING);
        m.setRejectReason(null);
        merchantMapper.updateById(m);

        Object quals = body.get("qualifications");
        if (quals instanceof List<?> list) {
            for (Object q : list) {
                if (q instanceof Map<?, ?> raw) {
                    Map<String, Object> qm = new java.util.HashMap<>();
                    raw.forEach((k, v) -> qm.put(String.valueOf(k), v == null ? "" : v));
                    Qualification qf = new Qualification();
                    qf.setMerchantId(m.getId());
                    qf.setMerchantNo(m.getMerchantNo());
                    qf.setType(str(qm, "type").isBlank() ? "business_license" : str(qm, "type"));
                    qf.setName(str(qm, "name").isBlank() ? "资质文件" : str(qm, "name"));
                    qf.setUrl(str(qm, "url"));
                    qf.setAuditStatus(0);
                    qualificationMapper.insert(qf);
                }
            }
        }
        return Result.ok("入驻资料已提交，等待平台审核").add("shopNum", m.getMerchantNo()).add("shopState", m.getStatus());
    }

    /** 入驻审核状态 + 资质列表 */
    public Result applyStatus() {
        Merchant m = currentMerchant();
        Result r = Result.ok();
        r.add("shopNum", m.getMerchantNo());
        r.add("shopState", m.getStatus());
        r.add("rejectReason", m.getRejectReason() == null ? "" : m.getRejectReason());
        r.add("auditTime", m.getAuditTime() == null ? "" : m.getAuditTime());
        r.add("shop", toShopMap(m));
        List<Map<String, Object>> quals = new ArrayList<>();
        for (Qualification q : qualificationMapper.selectList(new QueryWrapper<Qualification>().eq("merchant_no", m.getMerchantNo()))) {
            quals.add(Map.of("id", q.getId(), "type", q.getType(), "name", q.getName(),
                    "url", q.getUrl() == null ? "" : q.getUrl(), "auditStatus", q.getAuditStatus()));
        }
        r.add("qualifications", quals);
        return r;
    }

    /** 店铺信息维护 */
    public Result updateShop(Map<String, Object> body) {
        Merchant m = currentMerchant();
        if (!StatusConst.merchantActive(m.getStatus())) {
            throw new ApiException("店铺当前不可经营，请先通过入驻审核");
        }
        String pwd = str(body, "password");
        if (!pwd.isBlank()) {
            m.setPassword(pwd);
        }
        m.setShopName(str(body, "shopName").isBlank() ? m.getShopName() : str(body, "shopName"));
        m.setShopPhone(str(body, "shopPhone").isBlank() ? m.getShopPhone() : str(body, "shopPhone"));
        m.setShopAddress(str(body, "shopAddress").isBlank() ? m.getShopAddress() : str(body, "shopAddress"));
        m.setContactName(str(body, "contactName").isBlank() ? m.getContactName() : str(body, "contactName"));
        if (!str(body, "categoryName").isBlank()) {
            m.setCategoryName(str(body, "categoryName"));
        }
        merchantMapper.updateById(m);
        return Result.ok("店铺信息已更新").add("shop", toShopMap(m));
    }

    public Result shopInfo() {
        return Result.ok().add("shop", toShopMap(currentMerchant()));
    }

    private Map<String, Object> toShopMap(Merchant m) {
        // 注意：Map.of 不允许 null 值，而店铺地址/联系人等字段允许为空（如未完善资料的商户），
        // 直接使用 Map.of 会抛 NPE 导致接口 500（前端提示“系统繁忙”），故此处统一做空值兜底。
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("shopNum", m.getMerchantNo());
        map.put("shopName", m.getShopName() == null ? "" : m.getShopName());
        map.put("shopPhone", m.getShopPhone() == null ? "" : m.getShopPhone());
        map.put("shopAddress", m.getShopAddress() == null ? "" : m.getShopAddress());
        map.put("contactName", m.getContactName() == null ? "" : m.getContactName());
        map.put("categoryName", m.getCategoryName() == null ? "" : m.getCategoryName());
        map.put("status", m.getStatus());
        map.put("createTime", m.getCreateTime() == null ? "" : m.getCreateTime());
        return map;
    }

    // ---------- 商户商品管理 ----------

    public Result productList(Map<String, Object> body) {
        Merchant m = currentMerchant();
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        int status = intv(body, "status", -99);
        
        QueryWrapper<Product> w = new QueryWrapper<Product>()
                .eq("merchant_no", m.getMerchantNo())
                .ne("status", StatusConst.PRODUCT_DELETED)
                .eq(status != -99, "status", status);
        long total = productMapper.selectCount(w);
        List<Product> list = productMapper.selectList(w.orderByDesc("create_time").last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::productBrief).toList();
        return Result.ok().page(total, current, size, records);
    }

    public Result productCreate(Map<String, Object> body) {
        Merchant m = currentMerchant();
        if (!StatusConst.merchantActive(m.getStatus())) {
            throw new ApiException("请先通过入驻审核再发布商品");
        }
        String title = str(body, "title");
        if (title.isBlank()) {
            throw new ApiException("请填写商品标题");
        }
        Product p = new Product();
        p.setProductNo(NoGen.next("P"));
        p.setMerchantId(m.getId());
        p.setMerchantNo(m.getMerchantNo());
        p.setShopName(m.getShopName());
        fillProduct(p, body);
        productMapper.insert(p);
        fillSkus(p, body);
        cacheService.evictHome();
        return Result.ok("商品发布成功").add("productId", p.getId());
    }

    public Result productUpdate(Map<String, Object> body) {
        Merchant m = currentMerchant();
        long id = longv(body, "id");
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new ApiException("商品不存在");
        }
        if (!p.getMerchantNo().equals(m.getMerchantNo())) {
            throw ApiException.unauthorized();
        }
        fillProduct(p, body);
        productMapper.updateById(p);
        fillSkus(p, body);
        cacheService.evictHome();
        return Result.ok("商品已更新");
    }

    /**
     * 商家设置某个 SKU 的库存量（绝对值）：实时落库，同步 Redis 镜像、商品聚合库存与库存流水。
     */
    public Result productStock(Map<String, Object> body) {
        Merchant m = currentMerchant();
        long skuId = longv(body, "skuId");
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ApiException("商品规格不存在");
        }
        Product p = productMapper.selectById(sku.getProductId());
        if (p == null || !p.getMerchantNo().equals(m.getMerchantNo())) {
            throw ApiException.unauthorized();
        }
        int stock = intv(body, "stock", -1);
        if (stock < 0) {
            throw new ApiException("库存量不能小于0");
        }
        int before = sku.getStock();
        if (stock != before) {
            sku.setStock(stock);
            productSkuMapper.updateById(sku);
            redisTools.initStock(skuId, stock);
            InventoryLog logRow = new InventoryLog();
            logRow.setSkuId(skuId);
            logRow.setProductId(p.getId());
            logRow.setChangeType("商家调库存");
            logRow.setChangeQty(stock - before);
            logRow.setBeforeStock(before);
            logRow.setAfterStock(stock);
            logRow.setRemark("商家在编辑商品页设置库存");
            inventoryLogMapper.insert(logRow);
            // 商品聚合库存 = 各 SKU 库存之和
            int totalStock = productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))
                    .stream().mapToInt(ProductSku::getStock).sum();
            p.setStock(totalStock);
            productMapper.updateById(p);
        }
        return Result.ok("库存已更新").add("stock", stock);
    }

    public Result productStatus(Map<String, Object> body) {
        Merchant m = currentMerchant();
        long id = longv(body, "id");
        int status = intv(body, "status", 1);
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new ApiException("商品不存在");
        }
        if (!p.getMerchantNo().equals(m.getMerchantNo())) {
            throw ApiException.unauthorized();
        }
        p.setStatus(status);
        productMapper.updateById(p);
        cacheService.evictHome();
        return Result.ok(status == 1 ? "商品已上架" : "商品已下架");
    }

    public Result productDelete(Map<String, Object> body) {
        Merchant m = currentMerchant();
        long id = longv(body, "id");
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new ApiException("商品不存在");
        }
        if (!p.getMerchantNo().equals(m.getMerchantNo())) {
            throw ApiException.unauthorized();
        }
        p.setStatus(StatusConst.PRODUCT_DELETED);
        productMapper.updateById(p);
        cacheService.evictHome();
        return Result.ok("商品已删除");
    }

    private void fillProduct(Product p, Map<String, Object> body) {
        p.setTitle(str(body, "title"));
        p.setSubTitle(str(body, "subTitle"));
        p.setCategoryId(longv(body, "categoryId") == 0 ? 1L : longv(body, "categoryId"));
        p.setMainImage(str(body, "mainImage").isBlank() ? "https://picsum.photos/seed/default/600/600" : str(body, "mainImage"));
        p.setImages(str(body, "images").isBlank() ? "[\"" + p.getMainImage() + "\"]" : str(body, "images"));
        p.setPrice(dec(body, "price", p.getPrice()));
        p.setOriginalPrice(dec(body, "originalPrice", p.getOriginalPrice()));
        p.setDetail(str(body, "detail"));
        p.setIsRecommend(intv(body, "isRecommend", p.getIsRecommend()));
    }

    /** 重建 SKU 列表（demo 简单实现：清空重建，库存初始化进 Redis） */
    private void fillSkus(Product p, Map<String, Object> body) {
        for (ProductSku x : productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))) {
            productSkuMapper.deleteById(x.getId());
        }
        Object skuObj = body.get("skus");
        List<Map<String, Object>> skuList;
        if (skuObj instanceof List<?> list && !list.isEmpty()) {
            skuList = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    Map<String, Object> m = new java.util.HashMap<>();
                    map.forEach((k, v) -> m.put(String.valueOf(k), v));
                    skuList.add(m);
                }
            }
        } else {
            skuList = List.of(Map.of("specName", "默认", "specValue", "默认规格", "price", p.getPrice(), "stock", p.getStock()));
        }
        p.getSkus().clear();
        for (Map<String, Object> sm : skuList) {
            ProductSku s = new ProductSku();
            s.setProductId(p.getId());
            s.setSpecName(str(sm, "specName").isBlank() ? "默认" : str(sm, "specName"));
            s.setSpecValue(str(sm, "specValue").isBlank() ? "默认规格" : str(sm, "specValue"));
            BigDecimal price = sm.get("price") instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(sm.get("price")));
            s.setPrice(price);
            int stock = intv(sm, "stock", 0);
            s.setStock(stock);
            s.setImage(str(sm, "image").isBlank() ? p.getMainImage() : str(sm, "image"));
            productSkuMapper.insert(s);
            p.getSkus().add(s);
            redisTools.initStock(s.getId(), stock); // Redis 库存镜像初始化
        }
        productMapper.updateById(p);
    }

    private Map<String, Object> productBrief(Product p) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("productNo", p.getProductNo());
        map.put("title", p.getTitle());
        map.put("subTitle", p.getSubTitle());
        map.put("mainImage", p.getMainImage());
        map.put("price", p.getPrice());
        map.put("originalPrice", p.getOriginalPrice());
        map.put("stock", p.getStock());
        map.put("sales", p.getSales());
        map.put("status", p.getStatus());
        map.put("isRecommend", p.getIsRecommend());
        map.put("categoryId", p.getCategoryId());
        map.put("createTime", p.getCreateTime());
        List<Map<String, Object>> skus = new ArrayList<>();
        for (ProductSku s : productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))) {
            skus.add(Map.of("id", s.getId(), "specName", s.getSpecName(), "specValue", s.getSpecValue(),
                    "price", s.getPrice(), "stock", s.getStock()));
        }
        map.put("skus", skus);
        return map;
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

    private BigDecimal dec(Map<String, Object> body, String key, BigDecimal def) {
        Object v = body.get(key);
        if (v == null) return def;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private final com.mall.common.redis.RedisTools redisTools;
    private final InventoryLogMapper inventoryLogMapper;
    private final CacheService cacheService;
}