package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.AuthService;
import com.mall.mgr.service.MerchantService;
import com.mall.mgr.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商户端接口：注册/登录、入驻申请与状态、店铺信息、商品管理、店铺订单处理。
 * 统一 POST + JSON，经网关(10010)验签后进入。
 */
@RestController
@RequestMapping("/mch")
@RequiredArgsConstructor
public class MchController {

    private final AuthService authService;
    private final MerchantService merchantService;
    private final OrderService orderService;

    // ---------- 注册 / 登录（网关白名单，不要求登录态）----------

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, Object> body) {
        return authService.mchRegister(body);
    }

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> body) {
        return authService.mchLogin(body);
    }

    // ---------- 入驻 ----------

    @PostMapping("/apply")
    public Result apply(@RequestBody Map<String, Object> body) {
        return merchantService.apply(body);
    }

    @PostMapping("/apply/status")
    public Result applyStatus(@RequestBody Map<String, Object> body) {
        return merchantService.applyStatus();
    }

    // ---------- 店铺信息 ----------

    @PostMapping("/shop/info")
    public Result shopInfo(@RequestBody Map<String, Object> body) {
        return merchantService.shopInfo();
    }

    @PostMapping("/shop/update")
    public Result shopUpdate(@RequestBody Map<String, Object> body) {
        return merchantService.updateShop(body);
    }

    // ---------- 商品管理 ----------

    @PostMapping("/product/list")
    public Result productList(@RequestBody Map<String, Object> body) {
        return merchantService.productList(body);
    }

    @PostMapping("/product/create")
    public Result productCreate(@RequestBody Map<String, Object> body) {
        return merchantService.productCreate(body);
    }

    @PostMapping("/product/update")
    public Result productUpdate(@RequestBody Map<String, Object> body) {
        return merchantService.productUpdate(body);
    }

    @PostMapping("/product/status")
    public Result productStatus(@RequestBody Map<String, Object> body) {
        return merchantService.productStatus(body);
    }

    /** 库存设置：商家直接设置某个 SKU 的库存量，实时生效 */
    @PostMapping("/product/stock")
    public Result productStock(@RequestBody Map<String, Object> body) {
        return merchantService.productStock(body);
    }

    @PostMapping("/product/delete")
    public Result productDelete(@RequestBody Map<String, Object> body) {
        return merchantService.productDelete(body);
    }

    // ---------- 订单处理 ----------

    @PostMapping("/order/list")
    public Result orderList(@RequestBody Map<String, Object> body) {
        return orderService.merchantList(body);
    }

    @PostMapping("/order/detail")
    public Result orderDetail(@RequestBody Map<String, Object> body) {
        return orderService.merchantDetail(body);
    }

    @PostMapping("/order/ship")
    public Result ship(@RequestBody Map<String, Object> body) {
        return orderService.ship(body);
    }
}