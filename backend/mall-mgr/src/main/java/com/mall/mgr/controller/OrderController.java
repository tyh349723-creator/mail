package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户订单接口：预下单试算、下单、订单查询与操作、售后。
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public Result preview(@RequestBody Map<String, Object> body) {
        return orderService.preview(body);
    }

    @PostMapping("/create")
    public Result create(@RequestBody Map<String, Object> body) {
        return orderService.create(body);
    }

    @PostMapping("/user/list")
    public Result userList(@RequestBody Map<String, Object> body) {
        return orderService.userList(body);
    }

    @PostMapping("/user/detail")
    public Result userDetail(@RequestBody Map<String, Object> body) {
        return orderService.userDetail(body);
    }

    @PostMapping("/cancel")
    public Result cancel(@RequestBody Map<String, Object> body) {
        return orderService.cancel(body);
    }

    @PostMapping("/confirm")
    public Result confirm(@RequestBody Map<String, Object> body) {
        return orderService.confirm(body);
    }

    @PostMapping("/refund/apply")
    public Result refundApply(@RequestBody Map<String, Object> body) {
        return orderService.refundApply(body);
    }

    @PostMapping("/refund/list")
    public Result refundList(@RequestBody Map<String, Object> body) {
        return orderService.userRefunds(body);
    }

    @PostMapping("/refund/cancel")
    public Result refundCancel(@RequestBody Map<String, Object> body) {
        return orderService.refundCancel(body);
    }
}