package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.mgr.domain.Advert;
import com.mall.mgr.domain.AdvertSlot;
import com.mall.mgr.domain.Category;
import com.mall.mgr.domain.Product;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 运营管理：商品分类维护、广告位与广告配置（平台管理端）。
 */
@Service
@RequiredArgsConstructor
public class OperationService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final AdvertMapper advertMapper;
    private final AdvertSlotMapper advertSlotMapper;
    private final CacheService cacheService;

    // ---------- 分类 ----------

    public Result categoryTree() {
        List<Map<String, Object>> tree = buildTree(0L);
        return Result.ok().add("list", tree);
    }

    private List<Map<String, Object>> buildTree(Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Category> all = categoryMapper.selectList(null);
        all.sort(Comparator.comparingInt(Category::getSort));
        for (Category c : all) {
            if (Objects.equals(c.getParentId(), parentId)) {
                Map<String, Object> m = catMap(c);
                List<Map<String, Object>> children = buildTree(c.getId());
                if (!children.isEmpty()) {
                    m.put("children", children);
                }
                result.add(m);
            }
        }
        return result;
    }

    public Result categorySave(Map<String, Object> body) {
        long id = longv(body, "id");
        String name = str(body, "name");
        if (name.isBlank()) {
            throw new ApiException("分类名称不能为空");
        }
        Category c = id > 0 ? categoryMapper.selectById(id) : new Category();
        if (c == null) {
            throw new ApiException("分类不存在");
        }
        c.setName(name);
        long parentId = longv(body, "parentId");
        c.setParentId(parentId);
        c.setLevel(parentId == 0 ? 1 : 2);
        c.setSort(intv(body, "sort", 0));
        c.setIcon(str(body, "icon"));
        if (body.containsKey("status")) {
            c.setStatus(intv(body, "status", 1));
        } else if (c.getStatus() == 0) {
            c.setStatus(1);
        }
        if (id > 0) {
            categoryMapper.updateById(c);
        } else {
            categoryMapper.insert(c);
        }
        cacheService.evictCategoryChange();
        return Result.ok("分类已保存").add("categoryId", c.getId());
    }

    public Result categoryDelete(Map<String, Object> body) {
        long id = longv(body, "id");
        long childCount = categoryMapper.selectCount(new QueryWrapper<Category>().eq("parent_id", id));
        if (childCount > 0) {
            throw new ApiException("请先删除子分类");
        }
        long productCount = productMapper.selectCount(new QueryWrapper<Product>().eq("category_id", id));
        if (productCount > 0) {
            throw new ApiException("该分类下存在商品，无法删除");
        }
        categoryMapper.deleteById(id);
        cacheService.evictCategoryChange();
        return Result.ok("分类已删除");
    }

    public Result categoryStatus(Map<String, Object> body) {
        long id = longv(body, "id");
        int status = intv(body, "status", 1);
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new ApiException("分类不存在");
        }
        c.setStatus(status);
        categoryMapper.updateById(c);
        cacheService.evictCategoryChange();
        return Result.ok(status == 1 ? "分类已启用" : "分类已停用");
    }

    // ---------- 广告位 ----------

    public Result advertSlots() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdvertSlot s : advertSlotMapper.selectList(null)) {
            list.add(Map.of("id", s.getId(), "slotName", s.getSlotName(), "slotCode", s.getSlotCode(),
                    "position", s.getPosition(), "status", s.getStatus()));
        }
        return Result.ok().add("list", list);
    }

    public Result advertSlotSave(Map<String, Object> body) {
        long id = longv(body, "id");
        AdvertSlot s = id > 0 ? advertSlotMapper.selectById(id) : null;
        if (s == null) {
            s = new AdvertSlot();
        }
        s.setSlotName(str(body, "slotName"));
        s.setSlotCode(str(body, "slotCode"));
        s.setPosition(str(body, "position"));
        if (body.containsKey("status")) {
            s.setStatus(intv(body, "status", 1));
        }
        if (s.getId() != null) {
            advertSlotMapper.updateById(s);
        } else {
            advertSlotMapper.insert(s);
        }
        return Result.ok("广告位已保存");
    }

    public Result advertSlotStatus(Map<String, Object> body) {
        long id = longv(body, "id");
        int status = intv(body, "status", 1);
        AdvertSlot s = advertSlotMapper.selectById(id);
        if (s != null) {
            s.setStatus(status);
            advertSlotMapper.updateById(s);
        }
        return Result.ok("广告位状态已更新");
    }

    // ---------- 广告 ----------

    public Result advertList(Map<String, Object> body) {
        long slotId = longv(body, "slotId");
        int status = intv(body, "status", -99);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<Advert> w = new QueryWrapper<Advert>()
                .eq(slotId != 0, "slot_id", slotId)
                .eq(status != -99, "status", status);
        long total = advertMapper.selectCount(w);
        List<Advert> list = advertMapper.selectList(w.orderByAsc("sort").last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::advertView).toList();
        return Result.ok().page(total, current, size, records);
    }

    public Result advertSave(Map<String, Object> body) {
        long id = longv(body, "id");
        Advert a = id > 0 ? advertMapper.selectById(id) : null;
        if (a == null) {
            if (id > 0) {
                throw new ApiException("广告不存在");
            }
            a = new Advert();
        }
        a.setSlotId(longv(body, "slotId"));
        a.setTitle(str(body, "title"));
        a.setImageUrl(str(body, "imageUrl"));
        a.setLinkUrl(str(body, "linkUrl"));
        a.setSort(intv(body, "sort", 0));
        a.setStartTime(str(body, "startTime"));
        a.setEndTime(str(body, "endTime"));
        if (body.containsKey("status")) {
            a.setStatus(intv(body, "status", 1));
        } else if (a.getStatus() == 0 && id == 0) {
            a.setStatus(1);
        }
        if (id > 0) {
            advertMapper.updateById(a);
        } else {
            advertMapper.insert(a);
        }
        cacheService.evictHome();
        return Result.ok("广告已保存").add("advertId", a.getId());
    }

    public Result advertDelete(Map<String, Object> body) {
        advertMapper.deleteById(longv(body, "id"));
        cacheService.evictHome();
        return Result.ok("广告已删除");
    }

    public Result advertStatus(Map<String, Object> body) {
        long id = longv(body, "id");
        int status = intv(body, "status", 1);
        Advert a = advertMapper.selectById(id);
        if (a != null) {
            a.setStatus(status);
            advertMapper.updateById(a);
        }
        cacheService.evictHome();
        return Result.ok(status == 1 ? "广告已上架" : "广告已下架");
    }

    private Map<String, Object> advertView(Advert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("slotId", a.getSlotId());
        AdvertSlot slot = a.getSlotId() == null ? null : advertSlotMapper.selectById(a.getSlotId());
        m.put("slotName", slot == null ? "" : slot.getSlotName());
        m.put("title", a.getTitle());
        m.put("imageUrl", a.getImageUrl());
        m.put("linkUrl", a.getLinkUrl());
        m.put("sort", a.getSort());
        m.put("startTime", a.getStartTime() == null ? "" : a.getStartTime());
        m.put("endTime", a.getEndTime() == null ? "" : a.getEndTime());
        m.put("status", a.getStatus());
        return m;
    }

    private Map<String, Object> catMap(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("parentId", c.getParentId());
        m.put("level", c.getLevel());
        m.put("sort", c.getSort());
        m.put("icon", c.getIcon());
        m.put("status", c.getStatus());
        return m;
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