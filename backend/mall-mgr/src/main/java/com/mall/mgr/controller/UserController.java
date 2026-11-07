package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.AuthService;
import com.mall.mgr.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端接口：注册/登录、个人资料、收货地址。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserProfileService userProfileService;

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, Object> body) {
        return authService.userRegister(body);
    }

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> body) {
        return authService.userLogin(body);
    }

    @PostMapping("/profile")
    public Result profile(@RequestBody Map<String, Object> body) {
        return userProfileService.profile();
    }

    @PostMapping("/profile/update")
    public Result profileUpdate(@RequestBody Map<String, Object> body) {
        return userProfileService.updateProfile(body);
    }

    // ---------- 收货地址 ----------

    @PostMapping("/address/list")
    public Result addressList(@RequestBody Map<String, Object> body) {
        return userProfileService.addressList();
    }

    @PostMapping("/address/create")
    public Result addressCreate(@RequestBody Map<String, Object> body) {
        return userProfileService.addressCreate(body);
    }

    @PostMapping("/address/update")
    public Result addressUpdate(@RequestBody Map<String, Object> body) {
        return userProfileService.addressUpdate(body);
    }

    @PostMapping("/address/delete")
    public Result addressDelete(@RequestBody Map<String, Object> body) {
        return userProfileService.addressDelete(body);
    }
}