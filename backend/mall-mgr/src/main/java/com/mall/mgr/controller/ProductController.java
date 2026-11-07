package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商品接口：首页聚合、列表/搜索、详情、分类树。
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/home")
    public Result home(@RequestBody(required = false) Map<String, Object> body) {
        return productService.home();
    }

    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> body) {
        return productService.list(body);
    }

    @PostMapping("/detail")
    public Result detail(@RequestBody Map<String, Object> body) {
        long id = body.get("id") == null ? 0 : Long.parseLong(String.valueOf(body.get("id")));
        return productService.detail(id);
    }

    @PostMapping("/category/tree")
    public Result categoryTree(@RequestBody(required = false) Map<String, Object> body) {
        return productService.categoryTree();
    }
}