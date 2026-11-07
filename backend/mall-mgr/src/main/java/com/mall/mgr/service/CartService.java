package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.mgr.domain.CartItem;
import com.mall.mgr.domain.Product;
import com.mall.mgr.domain.ProductSku;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 购物车服务（服务端按用户维度）。
 * <p>游客购物车由移动端本地离线缓存，登录成功后调用 /cart/merge 将本地数据按 SKU 合并进服务端购物车。</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    /** 当前登录用户购物车 */
    public Result list() {
        Long userId = currentUserId();
        List<Map<String, Object>> items = new ArrayList<>();
        for (CartItem c : cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("user_id", userId))) {
            items.add(itemView(c));
        }
        return Result.ok().add("items", items).add("totalQuantity", items.stream()
                .filter(i -> Boolean.TRUE.equals(i.get("checked")))
                .mapToInt(i -> (Integer) i.get("quantity")).sum());
    }

    /** 加入购物车（同 SKU 数量累加） */
    public Result add(Map<String, Object> body) {
        Long userId = currentUserId();
        long skuId = longv(body, "skuId");
        int quantity = Math.max(1, intv(body, "quantity", 1));
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ApiException("商品规格不存在");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null || product.getStatus() != StatusConst.PRODUCT_ON) {
            throw new ApiException("商品已下架");
        }
        CartItem item = cartItemMapper.selectOne(new QueryWrapper<CartItem>()
                .eq("user_id", userId).eq("sku_id", skuId));
        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
            cartItemMapper.updateById(item);
        } else {
            item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setSkuId(skuId);
            item.setProductTitle(product.getTitle());
            item.setProductImage(sku.getImage() == null ? product.getMainImage() : sku.getImage());
            item.setSpecInfo(sku.getSpecValue());
            item.setPrice(sku.getPrice());
            item.setQuantity(quantity);
            item.setChecked(true);
            cartItemMapper.insert(item);
        }
        return Result.ok("已加入购物车").add("cartCount", count(userId));
    }

    /** 修改数量/勾选 */
    public Result update(Map<String, Object> body) {
        Long userId = currentUserId();
        long id = longv(body, "id");
        CartItem c = cartItemMapper.selectById(id);
        if (c == null || !userId.equals(c.getUserId())) {
            throw new ApiException("购物车条目不存在");
        }
        if (body.containsKey("quantity")) {
            int q = intv(body, "quantity", 1);
            if (q <= 0) {
                cartItemMapper.deleteById(id);
                return Result.ok("已删除");
            }
            c.setQuantity(q);
        }
        if (body.containsKey("checked")) {
            c.setChecked(Boolean.TRUE.equals(body.get("checked")));
        }
        cartItemMapper.updateById(c);
        return Result.ok();
    }

    /** 删除条目 */
    public Result remove(Map<String, Object> body) {
        Long userId = currentUserId();
        long id = longv(body, "id");
        CartItem c = cartItemMapper.selectById(id);
        if (c != null && userId.equals(c.getUserId())) {
            cartItemMapper.deleteById(id);
        }
        return Result.ok("已删除");
    }

    public Result clear() {
        Long userId = currentUserId();
        for (CartItem c : cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("user_id", userId))) {
            cartItemMapper.deleteById(c.getId());
        }
        return Result.ok();
    }

    /**
     * 登录后合并离线购物车：clientItems [{skuId, quantity}]，与服务端按 SKU 累加。
     */
    public Result merge(Map<String, Object> body) {
        Long userId = currentUserId();
        Object items = body.get("items");
        if (items instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> raw) {
                    Map<String, Object> m = new HashMap<>();
                    raw.forEach((k, v) -> m.put(String.valueOf(k), v));
                    add(m);
                }
            }
        }
        List<Map<String, Object>> merged = new ArrayList<>();
        for (CartItem c : cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("user_id", userId))) {
            merged.add(itemView(c));
        }
        return Result.ok("购物车已合并").add("items", merged).add("cartCount", count(userId));
    }

    /** 购物车总数（角标） */
    public Result count() {
        return Result.ok().add("cartCount", count(currentUserId()));
    }

    private long count(Long userId) {
        return cartItemMapper.selectList(new QueryWrapper<CartItem>().eq("user_id", userId)).stream()
                .mapToLong(CartItem::getQuantity).sum();
    }

    /** 条目视图：价格以 SKU 实时价为准，缺货/失效标记 */
    private Map<String, Object> itemView(CartItem c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("productId", c.getProductId());
        m.put("skuId", c.getSkuId());
        m.put("title", c.getProductTitle());
        m.put("image", c.getProductImage());
        m.put("specInfo", c.getSpecInfo());
        m.put("price", c.getPrice());
        m.put("quantity", c.getQuantity());
        m.put("checked", c.isChecked());
        Product p = productMapper.selectById(c.getProductId());
        boolean onSale = p != null && p.getStatus() == StatusConst.PRODUCT_ON;
        m.put("invalid", !onSale);
        ProductSku sku = productSkuMapper.selectById(c.getSkuId());
        if (sku != null) {
            m.put("availableStock", sku.getStock());
        }
        if (p != null) {
            m.put("shopName", p.getShopName());
            m.put("merchantNo", p.getMerchantNo());
        }
        return m;
    }

    private Long currentUserId() {
        Long userId = CurrentUser.userId();
        if (userId == null) {
            throw ApiException.unauthorized();
        }
        return userId;
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