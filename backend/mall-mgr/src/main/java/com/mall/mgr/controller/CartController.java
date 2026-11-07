package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 购物车接口（服务端按用户维度；游客购物车由移动端本地缓存，登录后 /cart/merge 合并）。
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/list")
    public Result list(@RequestBody Map<String, Object> body) {
        return cartService.list();
    }

    @PostMapping("/add")
    public Result add(@RequestBody Map<String, Object> body) {
        return cartService.add(body);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Map<String, Object> body) {
        return cartService.update(body);
    }

    @PostMapping("/remove")
    public Result remove(@RequestBody Map<String, Object> body) {
        return cartService.remove(body);
    }

    @PostMapping("/clear")
    public Result clear(@RequestBody Map<String, Object> body) {
        return cartService.clear();
    }

    /** 登录后合并离线购物车 clientItems: [{skuId, quantity}] */
    @PostMapping("/merge")
    public Result merge(@RequestBody Map<String, Object> body) {
        return cartService.merge(body);
    }

    @PostMapping("/count")
    public Result count(@RequestBody Map<String, Object> body) {
        return cartService.count();
    }
}