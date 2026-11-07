package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.JwtUtil;
import com.mall.common.util.NoGen;
import com.mall.mgr.domain.AdminUser;
import com.mall.mgr.domain.MallUser;
import com.mall.mgr.domain.Merchant;
import com.mall.mgr.domain.Role;
import com.mall.mgr.mapper.AdminUserMapper;
import com.mall.mgr.mapper.MallUserMapper;
import com.mall.mgr.mapper.MerchantMapper;
import com.mall.mgr.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 认证服务：商户注册/登录、用户注册/登录、管理员登录。
 * 令牌：HMAC-SHA256 JWT，网关统一校验并注入用户头。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MerchantMapper merchantMapper;
    private final MallUserMapper mallUserMapper;
    private final RoleMapper roleMapper;
    private final AdminUserMapper adminUserMapper;

    @Value("${mall.token.secret}")
    private String secret;

    @Value("${mall.token.expire-seconds}")
    private long expireSeconds;

    // ---------- 商户 ----------

    /**
     * 商户自主注册（指导书 /mch/register）。
     * 注册即提交入驻资料：商户状态置【待审核】，平台管理员审核通过前不可经营；
     * 审核完成后由管理员流程写入 auditor / auditTime。
     */
    public Result mchRegister(Map<String, Object> body) {
        String shopName = str(body, "shopName");
        String shopPhone = str(body, "shopPhone");
        String shopAddress = str(body, "shopAddress");
        String username = require(body, "username");
        String password = require(body, "password");
        if (shopName.isBlank() || shopPhone.isBlank() || shopAddress.isBlank()) {
            throw new ApiException("请完整填写店铺名称、联系电话与经营地址");
        }
        if (merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("username", username)) != null) {
            throw new ApiException("账号已存在，请直接登录");
        }
        Merchant m = new Merchant();
        m.setMerchantNo(NoGen.next("M"));
        m.setUsername(username);
        m.setPassword(password);
        m.setShopName(shopName);
        m.setShopPhone(shopPhone);
        m.setShopAddress(shopAddress);
        m.setContactName(str(body, "contactName").isBlank() ? username : str(body, "contactName"));
        m.setStatus(StatusConst.MCH_PENDING);
        merchantMapper.insert(m);
        return Result.ok("商户注册成功，资料已提交平台审核").add("shopNum", m.getMerchantNo()).add("shopState", m.getStatus());
    }

    /** 商户登录（指导书 /mch/login）：返回 token 与店铺状态 */
    public Result mchLogin(Map<String, Object> body) {
        String username = str(body, "username");
        String password = str(body, "password");
        Merchant m = merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("username", username));
        if (m == null) {
            throw new ApiException("账号或密码错误");
        }
        if (!m.getPassword().equals(password)) {
            throw new ApiException("账号或密码错误");
        }
        if (m.getStatus() == StatusConst.MCH_DISABLED) {
            throw new ApiException("商户已被禁用，请联系平台");
        }
        Result r = Result.ok("登录成功");
        r.add("token", token(m.getUsername(), m.getId(), JwtUtil.TYPE_MERCHANT, m.getMerchantNo(), ""));
        r.add("shopNum", m.getMerchantNo());
        r.add("shopState", m.getStatus());
        r.add("shopName", m.getShopName());
        r.add("shopPhone", m.getShopPhone());
        return r;
    }

    // ---------- 用户端 ----------

    public Result userRegister(Map<String, Object> body) {
        String username = require(body, "username");
        String password = require(body, "password");
        if (mallUserMapper.selectOne(new QueryWrapper<MallUser>().eq("username", username)) != null) {
            throw new ApiException("账号已存在");
        }
        MallUser u = new MallUser();
        u.setUserNum(NoGen.next("U"));
        u.setUsername(username);
        u.setPassword(password);
        u.setNickname(str(body, "nickname").isBlank() ? username : str(body, "nickname"));
        u.setPhone(str(body, "phone"));
        u.setAvatar(str(body, "avatar"));
        u.setStatus(1);
        mallUserMapper.insert(u);
        return Result.ok("注册成功").add("userNum", u.getUserNum());
    }

    public Result userLogin(Map<String, Object> body) {
        MallUser u = mallUserMapper.selectOne(new QueryWrapper<MallUser>().eq("username", str(body, "username")));
        if (u == null || !u.getPassword().equals(str(body, "password"))) {
            throw new ApiException("账号或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new ApiException("账号已被禁用，请联系平台");
        }
        Result r = Result.ok("登录成功");
        r.add("token", token(u.getUsername(), u.getId(), JwtUtil.TYPE_USER, u.getUserNum(), ""));
        r.add("userNum", u.getUserNum());
        r.add("nickname", u.getNickname());
        r.add("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        return r;
    }

    // ---------- 管理员 ----------

    public Result adminLogin(Map<String, Object> body) {
        AdminUser a = adminUserMapper.selectOne(new QueryWrapper<AdminUser>().eq("username", str(body, "username")));
        if (a == null || !a.getPassword().equals(str(body, "password"))) {
            throw new ApiException("账号或密码错误");
        }
        if (a.getStatus() != 1) {
            throw new ApiException("账号已停用，请联系超级管理员");
        }
        List<String> perms = permsOf(a.getRoles());
        Result r = Result.ok("登录成功");
        r.add("token", token(a.getUsername(), a.getId(), JwtUtil.TYPE_ADMIN, "", a.getRoles()));
        r.add("nickname", a.getNickname());
        r.add("roles", a.getRoles() == null ? "" : a.getRoles().split(","));
        r.add("permissions", perms);
        return r;
    }

    public List<String> permsOf(String roleCodes) {
        if (roleCodes == null || roleCodes.isBlank()) {
            return List.of();
        }
        Set<String> perms = new LinkedHashSet<>();
        Set<String> codes = new HashSet<>(Arrays.asList(roleCodes.split(",")));
        for (Role role : roleMapper.selectList(new QueryWrapper<Role>().eq("status", 1))) {
            if (codes.contains(role.getRoleCode()) && role.getPerms() != null) {
                perms.addAll(Arrays.asList(role.getPerms().split(",")));
            }
        }
        return new ArrayList<>(perms);
    }

    /** 管理员信息（含权限） */
    public Result adminInfo() {
        Long adminId = currentAdminId();
        AdminUser a = adminUserMapper.selectById(adminId);
        if (a == null) {
            throw ApiException.unauthorized();
        }
        Result r = Result.ok();
        // 注意：id 是统一响应的成功码（"0"），不能放业务主键 —— 管理员主键用 adminId 承载
        r.add("adminId", a.getId());
        r.add("username", a.getUsername());
        r.add("nickname", a.getNickname());
        String roleCodes = a.getRoles();
        r.add("roles", roleCodes == null ? new String[0] : roleCodes.split(","));
        r.add("permissions", permsOf(roleCodes));
        return r;
    }

    public Long currentAdminId() {
        Long id = com.mall.common.util.CurrentUser.userId();
        if (id == null) {
            throw ApiException.unauthorized();
        }
        return id;
    }

    // ---------- 工具 ----------

    private String token(String username, Long id, String type, String num, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("uid", id);
        claims.put("type", type);
        claims.put("num", num == null ? "" : num);
        claims.put("roles", roles);
        return JwtUtil.create(claims, secret, expireSeconds);
    }

    private String require(Map<String, Object> body, String key) {
        String v = str(body, key);
        if (v.isBlank()) {
            throw new ApiException("参数缺失：" + key);
        }
        return v;
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }
}