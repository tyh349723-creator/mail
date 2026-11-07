package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.mgr.domain.Address;
import com.mall.mgr.domain.MallUser;
import com.mall.mgr.mapper.AddressMapper;
import com.mall.mgr.mapper.MallUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端：个人资料与收货地址。
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final MallUserMapper mallUserMapper;
    private final AddressMapper addressMapper;

    private MallUser currentUser() {
        Long id = CurrentUser.userId();
        if (id == null) {
            throw ApiException.unauthorized();
        }
        MallUser u = mallUserMapper.selectById(id);
        if (u == null) {
            throw new ApiException("用户不存在");
        }
        return u;
    }

    public Result profile() {
        MallUser u = currentUser();
        Result r = Result.ok();
        r.add("userId", u.getId());
        r.add("username", u.getUsername());
        r.add("userNum", u.getUserNum());
        r.add("nickname", u.getNickname());
        r.add("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        r.add("phone", u.getPhone() == null ? "" : u.getPhone());
        return r;
    }

    public Result updateProfile(Map<String, Object> body) {
        MallUser u = currentUser();
        String nick = str(body, "nickname");
        String avatar = str(body, "avatar");
        String phone = str(body, "phone");
        if (!nick.isBlank()) {
            u.setNickname(nick);
        }
        if (!avatar.isBlank()) {
            u.setAvatar(avatar);
        }
        if (!phone.isBlank()) {
            u.setPhone(phone);
        }
        String oldPwd = str(body, "oldPassword");
        String newPwd = str(body, "newPassword");
        if (!oldPwd.isBlank() || !newPwd.isBlank()) {
            if (!u.getPassword().equals(oldPwd)) {
                throw new ApiException("原密码不正确");
            }
            u.setPassword(newPwd);
        }
        mallUserMapper.updateById(u);
        return Result.ok("保存成功").add("nickname", u.getNickname()).add("avatar", u.getAvatar());
    }

    // ---------- 收货地址 ----------

    public Result addressList() {
        MallUser u = currentUser();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Address a : addressMapper.selectList(new QueryWrapper<Address>().eq("user_id", u.getId()))) {
            list.add(addrView(a));
        }
        return Result.ok().add("list", list);
    }

    public Result addressCreate(Map<String, Object> body) {
        MallUser u = currentUser();
        long count = addressMapper.selectCount(new QueryWrapper<Address>().eq("user_id", u.getId()));
        if (count >= 20) {
            throw new ApiException("地址数量已达上限");
        }
        Address a = new Address();
        a.setUserId(u.getId());
        fillAddr(a, body);
        if (Boolean.TRUE.equals(body.get("defaultFlag"))) {
            clearDefault(u.getId());
        }
        if (count == 0) {
            a.setDefaultFlag(true);
        }
        addressMapper.insert(a);
        return Result.ok("地址已保存").add("address", addrView(a));
    }

    public Result addressUpdate(Map<String, Object> body) {
        MallUser u = currentUser();
        long id = longv(body, "id");
        Address a = addressMapper.selectById(id);
        if (a == null || !u.getId().equals(a.getUserId())) {
            throw new ApiException("地址不存在");
        }
        fillAddr(a, body);
        if (Boolean.TRUE.equals(body.get("defaultFlag"))) {
            clearDefault(u.getId());
            a.setDefaultFlag(true);
        }
        addressMapper.updateById(a);
        return Result.ok("地址已更新").add("address", addrView(a));
    }

    public Result addressDelete(Map<String, Object> body) {
        MallUser u = currentUser();
        long id = longv(body, "id");
        Address a = addressMapper.selectById(id);
        if (a != null && u.getId().equals(a.getUserId())) {
            addressMapper.deleteById(id);
        }
        return Result.ok("地址已删除");
    }

    private void clearDefault(Long userId) {
        for (Address a : addressMapper.selectList(new QueryWrapper<Address>().eq("user_id", userId))) {
            if (!a.isDefaultFlag()) {
                continue;
            }
            a.setDefaultFlag(false);
            addressMapper.updateById(a);
        }
    }

    private void fillAddr(Address a, Map<String, Object> body) {
        a.setConsignee(str(body, "consignee"));
        a.setPhone(str(body, "phone"));
        a.setProvince(str(body, "province"));
        a.setCity(str(body, "city"));
        a.setDistrict(str(body, "district"));
        a.setDetail(str(body, "detail"));
        if (a.getConsignee().isBlank() || a.getPhone().isBlank() || a.getDetail().isBlank()) {
            throw new ApiException("请完整填写收货人、电话与详细地址");
        }
        if (body.get("defaultFlag") != null) {
            a.setDefaultFlag(Boolean.TRUE.equals(body.get("defaultFlag")));
        }
    }

    private Map<String, Object> addrView(Address a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("consignee", a.getConsignee());
        m.put("phone", a.getPhone());
        m.put("province", a.getProvince());
        m.put("city", a.getCity());
        m.put("district", a.getDistrict());
        m.put("detail", a.getDetail());
        m.put("full", joinAddr(a));
        m.put("defaultFlag", a.isDefaultFlag());
        return m;
    }

    private String joinAddr(Address a) {
        return (a.getProvince() == null ? "" : a.getProvince())
                + (a.getCity() == null ? "" : a.getCity())
                + (a.getDistrict() == null ? "" : a.getDistrict())
                + a.getDetail();
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
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