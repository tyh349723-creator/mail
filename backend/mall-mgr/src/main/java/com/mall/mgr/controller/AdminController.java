package com.mall.mgr.controller;

import com.mall.common.result.Result;
import com.mall.mgr.service.AdminService;
import com.mall.mgr.service.AuthService;
import com.mall.mgr.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台管理端接口（pc-admin）：登录认证、商户审核、订单/退款/渠道/提现管控、
 * 数据统计、系统管理（管理员/角色/权限/日志）、运营配置（分类/广告）。
 * 统一 POST + JSON，经网关验签并由 JWT 管理员身份注入。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final AdminService adminService;
    private final OperationService operationService;

    // ---------- 登录认证 ----------

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> body) {
        return authService.adminLogin(body);
    }

    @PostMapping("/auth/info")
    public Result authInfo(@RequestBody(required = false) Map<String, Object> body) {
        return authService.adminInfo();
    }

    // ---------- 商户审核管理 ----------

    @PostMapping("/mch/list")
    public Result mchList(@RequestBody Map<String, Object> body) {
        return adminService.merchantList(body);
    }

    @PostMapping("/mch/detail")
    public Result mchDetail(@RequestBody Map<String, Object> body) {
        return adminService.merchantDetail(body);
    }

    @PostMapping("/mch/audit")
    public Result mchAudit(@RequestBody Map<String, Object> body) {
        return adminService.merchantAudit(body);
    }

    @PostMapping("/mch/qualification/audit")
    public Result qualificationAudit(@RequestBody Map<String, Object> body) {
        return adminService.qualificationAudit(body);
    }

    @PostMapping("/mch/status")
    public Result mchStatus(@RequestBody Map<String, Object> body) {
        return adminService.merchantStatus(body);
    }

    @PostMapping("/mch/rate")
    public Result mchRate(@RequestBody Map<String, Object> body) {
        return adminService.merchantRate(body);
    }

    @PostMapping("/mch/rate/list")
    public Result mchRateList(@RequestBody Map<String, Object> body) {
        return adminService.merchantRateList(body);
    }

    // ---------- 订单 / 退款 ----------

    @PostMapping("/order/list")
    public Result orderList(@RequestBody Map<String, Object> body) {
        return adminService.orderList(body);
    }

    @PostMapping("/order/detail")
    public Result orderDetail(@RequestBody Map<String, Object> body) {
        return adminService.orderDetail(body);
    }

    /** 异常订单处理（支付失败/超时未支付人工关单） */
    @PostMapping("/order/handle")
    public Result orderHandle(@RequestBody Map<String, Object> body) {
        return adminService.orderHandle(body);
    }

    @PostMapping("/refund/list")
    public Result refundList(@RequestBody Map<String, Object> body) {
        return adminService.refundList(body);
    }

    @PostMapping("/refund/audit")
    public Result refundAudit(@RequestBody Map<String, Object> body) {
        return adminService.refundAudit(body);
    }

    // ---------- 渠道 / 提现 ----------

    @PostMapping("/channel/list")
    public Result channelList(@RequestBody Map<String, Object> body) {
        return adminService.channelList();
    }

    @PostMapping("/channel/config")
    public Result channelConfig(@RequestBody Map<String, Object> body) {
        return adminService.channelConfig(body);
    }

    @PostMapping("/channel/status")
    public Result channelStatus(@RequestBody Map<String, Object> body) {
        return adminService.channelStatus(body);
    }

    @PostMapping("/withdraw/list")
    public Result withdrawList(@RequestBody Map<String, Object> body) {
        return adminService.withdrawList(body);
    }

    @PostMapping("/withdraw/audit")
    public Result withdrawAudit(@RequestBody Map<String, Object> body) {
        return adminService.withdrawAudit(body);
    }

    // ---------- 数据统计 ----------

    @PostMapping("/statis/platform")
    public Result statisPlatform(@RequestBody(required = false) Map<String, Object> body) {
        return adminService.statisPlatform(body == null ? Map.of() : body);
    }

    @PostMapping("/statis/merchants")
    public Result statisMerchants(@RequestBody(required = false) Map<String, Object> body) {
        return adminService.statisMerchants(body == null ? Map.of() : body);
    }

    // ---------- 系统管理 ----------

    @PostMapping("/system/user/list")
    public Result userList(@RequestBody Map<String, Object> body) {
        return adminService.adminUserList(body);
    }

    @PostMapping("/system/user/add")
    public Result userAdd(@RequestBody Map<String, Object> body) {
        return adminService.adminUserAdd(body);
    }

    @PostMapping("/system/user/update")
    public Result userUpdate(@RequestBody Map<String, Object> body) {
        return adminService.adminUserUpdate(body);
    }

    @PostMapping("/system/user/reset-password")
    public Result userResetPassword(@RequestBody Map<String, Object> body) {
        return adminService.adminResetPassword(body);
    }

    // ---------- 用户管理（用户端账号） ----------

    @PostMapping("/user/list")
    public Result mallUserList(@RequestBody Map<String, Object> body) {
        return adminService.mallUserList(body);
    }

    @PostMapping("/user/detail")
    public Result mallUserDetail(@RequestBody Map<String, Object> body) {
        return adminService.mallUserDetail(body);
    }

    @PostMapping("/user/status")
    public Result mallUserStatus(@RequestBody Map<String, Object> body) {
        return adminService.mallUserStatus(body);
    }

    @PostMapping("/user/reset-password")
    public Result mallUserResetPassword(@RequestBody Map<String, Object> body) {
        return adminService.mallUserResetPassword(body);
    }

    @PostMapping("/role/list")
    public Result roleList(@RequestBody Map<String, Object> body) {
        return adminService.roleList();
    }

    @PostMapping("/role/save")
    public Result roleSave(@RequestBody Map<String, Object> body) {
        return adminService.roleSave(body);
    }

    @PostMapping("/role/delete")
    public Result roleDelete(@RequestBody Map<String, Object> body) {
        return adminService.roleDelete(body);
    }

    @PostMapping("/permission/tree")
    public Result permissionTree(@RequestBody(required = false) Map<String, Object> body) {
        return adminService.permissionTree();
    }

    @PostMapping("/system/log/list")
    public Result logList(@RequestBody Map<String, Object> body) {
        return adminService.operLogList(body);
    }

    @PostMapping("/system/config/list")
    public Result sysConfigList(@RequestBody(required = false) Map<String, Object> body) {
        return adminService.sysConfigList(body);
    }

    @PostMapping("/system/config/save")
    public Result sysConfigSave(@RequestBody Map<String, Object> body) {
        return adminService.sysConfigSave(body);
    }

    @PostMapping("/monitor/status")
    public Result monitorStatus(@RequestBody(required = false) Map<String, Object> body) {
        return adminService.monitorStatus(body);
    }

    // ---------- 运营配置（分类 / 广告）----------

    @PostMapping("/category/tree")
    public Result categoryTree(@RequestBody(required = false) Map<String, Object> body) {
        return operationService.categoryTree();
    }

    @PostMapping("/category/save")
    public Result categorySave(@RequestBody Map<String, Object> body) {
        return operationService.categorySave(body);
    }

    @PostMapping("/category/delete")
    public Result categoryDelete(@RequestBody Map<String, Object> body) {
        return operationService.categoryDelete(body);
    }

    @PostMapping("/category/status")
    public Result categoryStatus(@RequestBody Map<String, Object> body) {
        return operationService.categoryStatus(body);
    }

    @PostMapping("/advert/slots")
    public Result advertSlots(@RequestBody(required = false) Map<String, Object> body) {
        return operationService.advertSlots();
    }

    @PostMapping("/advert/slot/save")
    public Result advertSlotSave(@RequestBody Map<String, Object> body) {
        return operationService.advertSlotSave(body);
    }

    @PostMapping("/advert/slot/status")
    public Result advertSlotStatus(@RequestBody Map<String, Object> body) {
        return operationService.advertSlotStatus(body);
    }

    @PostMapping("/advert/list")
    public Result advertList(@RequestBody Map<String, Object> body) {
        return operationService.advertList(body);
    }

    @PostMapping("/advert/save")
    public Result advertSave(@RequestBody Map<String, Object> body) {
        return operationService.advertSave(body);
    }

    @PostMapping("/advert/delete")
    public Result advertDelete(@RequestBody Map<String, Object> body) {
        return operationService.advertDelete(body);
    }

    @PostMapping("/advert/status")
    public Result advertStatus(@RequestBody Map<String, Object> body) {
        return operationService.advertStatus(body);
    }
}