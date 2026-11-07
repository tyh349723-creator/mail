package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.mgr.domain.*;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 平台管理端服务：RBAC 鉴权/角色权限、商户审核与状态管控、订单管控、退款/渠道/提现聚合（跨服务代理）、
 * 平台数据统计（支付侧数据从 alipay 聚合）、系统运维（系统参数/操作日志/接口运行状态监控）。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AuthService authService;
    private final OrderService orderService;
    private final ProductService productService;
    private final AlipayClient alipayClient;
    private final MonitorService monitorService;
    private final AdminUserMapper adminUserMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OperLogMapper operLogMapper;
    private final QualificationMapper qualificationMapper;
    private final RoleMapper roleMapper;
    private final SysConfigMapper sysConfigMapper;
    private final MallUserMapper mallUserMapper;
    private final AddressMapper addressMapper;

    /** 当前管理员 + 权限校验 */
    private AdminUser requireAdmin(String perm) {
        if (!CurrentUser.isAdmin()) {
            throw ApiException.unauthorized();
        }
        Long adminId = CurrentUser.userId();
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw ApiException.unauthorized();
        }
        if ("super_admin".equals(admin.getRoles())) {
            return admin; // 超管免验
        }
        if (perm != null && !authService.permsOf(admin.getRoles()).contains(perm)) {
            throw new ApiException("无操作权限：" + perm);
        }
        return admin;
    }

    /** 记录操作日志 */
    private void log(AdminUser admin, String action, String target, String detail) {
        try {
            OperLog log = new OperLog();
            log.setAdminId(admin.getId());
            log.setAdminName(admin.getNickname());
            log.setAction(action);
            log.setTarget(target == null ? "" : target);
            log.setDetail(detail == null ? "" : detail);
            log.setIp(CurrentUser.header("X-Real-IP") == null ? "127.0.0.1" : CurrentUser.header("X-Real-IP"));
            operLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响主流程
        }
    }

    // ---------- 商户审核管理 ----------

    public Result merchantList(Map<String, Object> body) {
        AdminUser admin = requireAdmin("merchant:review");
        int status = intv(body, "status", -99);
        String keyword = str(body, "keyword");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        
        QueryWrapper<Merchant> wrapper = new QueryWrapper<Merchant>()
                .eq(status != -99, "status", status)
                .and(!keyword.isBlank(), w -> w.like("shop_name", keyword).or().like("shop_phone", keyword).or().like("merchant_no", keyword))
                .orderByDesc("create_time");
        
        Long total = merchantMapper.selectCount(wrapper);
        List<Merchant> list = merchantMapper.selectList(wrapper.last("LIMIT " + (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::merchantView).toList();
        return Result.ok().page(total, current, size, records);
    }

    public Result merchantDetail(Map<String, Object> body) {
        AdminUser admin = requireAdmin("merchant:review");
        long id = longv(body, "id");
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new ApiException("商户不存在");
        }
        Result r = Result.ok().add("merchant", merchantView(m));
        List<Map<String, Object>> quals = new ArrayList<>();
        List<Qualification> qualifications = qualificationMapper.selectList(new QueryWrapper<Qualification>().eq("merchant_no", m.getMerchantNo()));
        for (Qualification q : qualifications) {
            quals.add(Map.of("id", q.getId(), "type", q.getType(), "name", q.getName(),
                    "url", q.getUrl() == null ? "" : q.getUrl(), "auditStatus", q.getAuditStatus(),
                    "auditRemark", q.getAuditRemark() == null ? "" : q.getAuditRemark()));
        }
        r.add("qualifications", quals);
        return r;
    }

    /** 入驻审核（指导书 /admin/mch/audit）：入参 shopNum + shopState(1通过/2驳回)；兼容 id/merchantId + approve */
    public Result merchantAudit(Map<String, Object> body) {
        AdminUser admin = requireAdmin("merchant:review");
        long id = longv(body, "id");
        if (id <= 0) {
            id = longv(body, "merchantId");
        }
        if (id <= 0) {
            String shopNum = str(body, "shopNum");
            Merchant byNum = merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("merchant_no", shopNum));
            if (byNum == null) {
                throw new ApiException("参数缺少商户标识（id/shopNum）");
            }
            id = byNum.getId();
        }
        // 驳回判定：显式 shopState=2 驳回；否则按 approve 布尔/数字判定（如 approve=false 须正确驳回到 2，
        // 不能因 String.valueOf(false) 解析失败回退默认值而误判为通过）
        boolean reject = intv(body, "shopState", 1) == 2;
        if (!reject && body.containsKey("approve")) {
            Object a = body.get("approve");
            boolean approve = a instanceof Boolean b ? b : intv(body, "approve", 1) == 1;
            reject = !approve;
        }
        String remark = str(body, "auditRemark").isBlank() ? str(body, "rejectReason") : str(body, "auditRemark");
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new ApiException("商户不存在");
        }
        if (reject) {
            m.setStatus(StatusConst.MCH_REJECTED);
            m.setRejectReason(remark.isBlank() ? "资料不完整" : remark);
        } else {
            m.setStatus(StatusConst.MCH_NORMAL);
            m.setRejectReason(null);
        }
        m.setAuditor(admin.getNickname());
        m.setAuditTime(com.mall.common.util.TimeUtils.now());
        merchantMapper.updateById(m);
        log(admin, "商户审核", m.getMerchantNo(), reject ? "驳回：" + m.getRejectReason() : "通过");
        return Result.ok("商户审核操作成功");
    }

    /** 资质审查 */
    public Result qualificationAudit(Map<String, Object> body) {
        AdminUser admin = requireAdmin("merchant:qualification");
        long id = longv(body, "id");
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        String reason = str(body, "reason");
        Qualification q = qualificationMapper.selectById(id);
        if (q == null) {
            throw new ApiException("资质不存在");
        }
        q.setAuditStatus(approve ? 1 : 2);
        q.setAuditRemark(approve ? "通过" : (reason.isBlank() ? "资料不符" : reason));
        qualificationMapper.updateById(q);
        log(admin, "资质审查", q.getName(), approve ? "通过" : "驳回：" + reason);
        return Result.ok("资质审查操作成功");
    }

    /** 店铺状态管理（停用/启用） */
    public Result merchantStatus(Map<String, Object> body) {
        AdminUser admin = requireAdmin("merchant:status");
        long id = longv(body, "id");
        int status = intv(body, "status", 1);
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new ApiException("商户不存在");
        }
        m.setStatus(status);
        merchantMapper.updateById(m);
        log(admin, "店铺状态", m.getShopName(), status == 1 ? "启用" : status == 3 ? "禁用" : "状态[" + status + "]");
        return Result.ok("店铺状态已更新");
    }

    /** 商户费率配置（指导书 /admin/mch/rate，归属支付侧） */
    public Result merchantRate(Map<String, Object> body) {
        AdminUser admin = requireAdmin("payment:list");
        Map<String, Object> res = alipayClient.channelRate(str(body, "shopNum"), str(body, "wayCode"), str(body, "mchRate"));
        check(res);
        log(admin, "费率配置", str(body, "shopNum"), str(body, "wayCode") + ":" + str(body, "mchRate"));
        return Result.ok("商户费率配置成功");
    }

    /** 商户差异化费率查询（管理端费率配置弹窗数据源） */
    public Result merchantRateList(Map<String, Object> body) {
        requireAdmin("payment:list");
        Map<String, Object> res = alipayClient.merchantRateList(str(body, "merchantNo"));
        check(res);
        return wrap(res);
    }

    // ---------- 订单/退款/渠道/提现（聚合支付侧） ----------

    public Result orderList(Map<String, Object> body) {
        requireAdmin("order:list");
        return orderService.platformList(body);
    }

    public Result orderDetail(Map<String, Object> body) {
        requireAdmin("order:list");
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        return Result.ok().add("order", orderView(o));
    }

    public Result refundList(Map<String, Object> body) {
        requireAdmin("order:refund");
        Map<String, Object> res = alipayClient.refundList(body);
        check(res);
        Result r = wrap(res);
        // 5.3.4 退款监管：附平台退款率汇总（大额审核/退款率统计）
        Map<String, Object> pay = alipayClient.statisPay();
        BigDecimal totalMoney = new BigDecimal(String.valueOf(pay.getOrDefault("totalMoney", 0)));
        BigDecimal refundMoney = new BigDecimal(String.valueOf(pay.getOrDefault("refundMoney", 0)));
        long refundCount = pay.get("refundCount") instanceof Number n ? n.longValue() : 0L;
        r.add("refundCount", refundCount);
        r.add("refundMoney", refundMoney);
        r.add("totalMoney", totalMoney);
        r.add("refundRate", totalMoney.signum() == 0 ? BigDecimal.ZERO
                : refundMoney.multiply(BigDecimal.valueOf(100)).divide(totalMoney, 1, java.math.RoundingMode.HALF_UP));
        return r;
    }

    /** 异常单处理（5.3.3：对 支付失败/超时未支付 等异常单人工关单并回补库存） */
    public Result orderHandle(Map<String, Object> body) {
        AdminUser admin = requireAdmin("order:list");
        Result r = orderService.adminHandle(body);
        log(admin, "异常订单处理", str(body, "orderNo"), str(body, "action").isBlank() ? "close" : str(body, "action"));
        return r;
    }

    public Result refundAudit(Map<String, Object> body) {
        AdminUser admin = requireAdmin("order:refund");
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        Map<String, Object> res = alipayClient.refundAudit(longv(body, "id"), approve, str(body, "rejectReason"));
        check(res);
        log(admin, "退款审核", "#" + longv(body, "id"), approve ? "同意" : "驳回");
        return wrap(res);
    }

    public Result channelList() {
        requireAdmin("payment:list");
        Map<String, Object> res = alipayClient.channelList();
        check(res);
        return wrap(res);
    }

    public Result channelConfig(Map<String, Object> body) {
        AdminUser admin = requireAdmin("payment:config");
        Map<String, Object> res = alipayClient.channelConfig(body);
        check(res);
        log(admin, "渠道配置", str(body, "channelCode"), "");
        return wrap(res);
    }

    public Result channelStatus(Map<String, Object> body) {
        AdminUser admin = requireAdmin("payment:list");
        Map<String, Object> res = alipayClient.channelStatus(longv(body, "id"), intv(body, "status", 1));
        check(res);
        log(admin, "渠道开关", "#" + longv(body, "id"), intv(body, "status", 1) == 1 ? "开启" : "关闭");
        return wrap(res);
    }

    public Result withdrawList(Map<String, Object> body) {
        requireAdmin("payment:list");
        Map<String, Object> res = alipayClient.withdrawList(body);
        check(res);
        return wrap(res);
    }

    public Result withdrawAudit(Map<String, Object> body) {
        AdminUser admin = requireAdmin("payment:list");
        boolean approve = Boolean.TRUE.equals(body.get("approve")) || intv(body, "approve", 0) == 1;
        Map<String, Object> res = alipayClient.withdrawAudit(longv(body, "id"), approve);
        check(res);
        log(admin, "提现审核", "#" + longv(body, "id"), approve ? "打款" : "驳回");
        return wrap(res);
    }

    // ---------- 数据统计（指导书 /statis/platform） ----------

    /** 平台交易统计：总额/笔数/渠道占比/趋势，支付数据聚合自 alipay；支持 begin/end 区间与 type 趋势粒度 */
    public Result statisPlatform(Map<String, Object> body) {
        requireAdmin("report:view");
        Map<String, Object> pay = alipayClient.statisPay(str(body, "begin"), str(body, "end"), str(body, "type"));
        Result r = Result.ok("统计成功");
        r.add("totalMoney", pay.getOrDefault("totalMoney", 0));
        r.add("totalCount", pay.getOrDefault("totalCount", 0));
        r.add("channelRatio", pay.getOrDefault("channelRatio", List.of()));
        r.add("sevenDayData", pay.getOrDefault("sevenDayData", List.of()));
        r.add("merchantCount", merchantMapper.selectCount(new QueryWrapper<Merchant>().eq("status", StatusConst.MCH_NORMAL)));
        r.add("orderToday", orderMapper.selectCount(new QueryWrapper<Order>()
                .ge("status", StatusConst.ORDER_PAID)
                .likeRight("create_time", com.mall.common.util.TimeUtils.today())));
        r.add("userCount", mallUserMapper.selectCount(null));
        return r;
    }

    /** 商户经营排行/分析（逐商户聚合支付侧交易数据，供 pc-admin 报表）；支持 begin/end 区间 */
    public Result statisMerchants(Map<String, Object> body) {
        requireAdmin("report:merchant");
        int limit = Math.min(intv(body, "limit", 10), 100);
        String begin = str(body, "begin");
        String end = str(body, "end");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Merchant m : merchantMapper.selectList(new QueryWrapper<Merchant>().eq("status", StatusConst.MCH_NORMAL))) {
            Map<String, Object> s = alipayClient.statisMchPay(m.getMerchantNo(), begin, end);
            if (!"0".equals(String.valueOf(s.get("id")))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantNo", m.getMerchantNo());
            item.put("shopName", m.getShopName());
            item.put("salesAmount", s.getOrDefault("totalMoney", 0));
            item.put("orderCount", s.getOrDefault("totalCount", 0));
            item.put("feeMoney", s.getOrDefault("feeMoney", 0));
            item.put("refundMoney", s.getOrDefault("refundMoney", 0));
            item.put("status", m.getStatus());
            list.add(item);
        }
        list.sort(Comparator.comparingDouble(
                (Map<String, Object> a) -> Double.parseDouble(String.valueOf(a.getOrDefault("salesAmount", 0))))
                .reversed());
        List<Map<String, Object>> top = list.subList(0, Math.min(limit, list.size()));
        return Result.ok().add("rank", top).add("list", list);
    }

    // ---------- 系统管理（RBAC） ----------

    public Result adminUserList(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:user");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        QueryWrapper<AdminUser> wrapper = new QueryWrapper<AdminUser>().orderByDesc("create_time");
        Long total = adminUserMapper.selectCount(wrapper);
        List<Map<String, Object>> list = adminUserMapper.selectList(wrapper.last("LIMIT " + (current - 1) * size + "," + size)).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("username", a.getUsername());
            m.put("nickname", a.getNickname());
            m.put("roles", a.getRoles() == null ? "" : a.getRoles());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            return m;
        }).toList();
        return Result.ok().page(total, current, size, list);
    }

    public Result adminUserAdd(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:user");
        if (adminUserMapper.selectCount(new QueryWrapper<AdminUser>().eq("username", str(body, "username"))) > 0) {
            throw new ApiException("账号已存在");
        }
        AdminUser a = new AdminUser();
        a.setUsername(str(body, "username"));
        a.setPassword(str(body, "password").isBlank() ? "123456" : str(body, "password"));
        a.setNickname(str(body, "nickname").isBlank() ? a.getUsername() : str(body, "nickname"));
        a.setRoles(str(body, "roles").isBlank() ? "operator" : str(body, "roles"));
        a.setStatus(1);
        adminUserMapper.insert(a);
        log(admin, "新增管理员", a.getUsername(), "");
        return Result.ok("创建成功");
    }

    public Result adminUserUpdate(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:user");
        AdminUser a = adminUserMapper.selectById(longv(body, "id"));
        if (a == null) {
            throw new ApiException("账号不存在");
        }
        if (!str(body, "nickname").isBlank()) {
            a.setNickname(str(body, "nickname"));
        }
        if (!str(body, "roles").isBlank()) {
            a.setRoles(str(body, "roles"));
        }
        adminUserMapper.updateById(a);
        return Result.ok("更新成功");
    }

    public Result adminResetPassword(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:user");
        AdminUser a = adminUserMapper.selectById(longv(body, "id"));
        if (a == null) {
            throw new ApiException("账号不存在");
        }
        a.setPassword(!str(body, "password").isBlank() ? str(body, "password") : "123456");
        adminUserMapper.updateById(a);
        return Result.ok("密码已重置");
    }

    public Result roleList() {
        AdminUser admin = requireAdmin("system:role");
        List<Map<String, Object>> list = roleMapper.selectList(new QueryWrapper<>()).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("roleName", r.getRoleName());
            m.put("roleCode", r.getRoleCode());
            m.put("description", r.getDescription());
            m.put("status", r.getStatus());
            m.put("perms", r.getPerms() == null ? "" : r.getPerms());
            return m;
        }).toList();
        return Result.ok().add("list", list);
    }

    public Result roleSave(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:role");
        long id = longv(body, "id");
        Role role = id > 0 ? roleMapper.selectById(id) : null;
        if (role == null) {
            role = new Role();
        }
        role.setRoleName(str(body, "roleName"));
        role.setRoleCode(str(body, "roleCode"));
        role.setDescription(str(body, "description"));
        role.setPerms(str(body, "perms"));
        role.setStatus(intv(body, "status", 1));
        if (role.getId() != null) {
            roleMapper.updateById(role);
        } else {
            roleMapper.insert(role);
        }
        return Result.ok("角色已保存");
    }

    public Result roleDelete(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:role");
        long id = longv(body, "id");
        Role r = roleMapper.selectById(id);
        if (r != null) {
            Long inUse = adminUserMapper.selectCount(new QueryWrapper<AdminUser>().like("roles", r.getRoleCode()));
            if (inUse > 0) {
                throw new ApiException("角色正在使用中，无法删除");
            }
            roleMapper.deleteById(id);
        }
        return Result.ok("角色已删除");
    }

    /** 全部权限码树（静态定义，与角色 perms 对应） */
    public Result permissionTree() {
        requireAdmin("system:permission");
        List<Map<String, Object>> tree = List.of(
                perm(1, "商户管理", 0, "merchant", List.of(
                        perm(11, "入驻审核", 1, "merchant:review", List.of()),
                        perm(12, "资质审查", 1, "merchant:qualification", List.of()),
                        perm(13, "店铺状态", 1, "merchant:status", List.of()))),
                perm(2, "订单管控", 0, "order", List.of(
                        perm(21, "订单查询", 1, "order:list", List.of()),
                        perm(22, "退款审核", 1, "order:refund", List.of()))),
                perm(3, "支付配置", 0, "payment", List.of(
                        perm(31, "渠道管理", 1, "payment:list", List.of()),
                        perm(32, "渠道配置", 1, "payment:config", List.of()))),
                perm(4, "统计报表", 0, "report", List.of(
                        perm(41, "报表查看", 1, "report:view", List.of()),
                        perm(42, "交易趋势", 1, "report:trade", List.of()),
                        perm(43, "商户分析", 1, "report:merchant", List.of()))),
                perm(5, "系统管理", 0, "system", List.of(
                        perm(51, "管理员", 1, "system:user", List.of()),
                        perm(52, "角色", 1, "system:role", List.of()),
                        perm(53, "权限", 1, "system:permission", List.of()),
                        perm(54, "操作日志", 1, "system:log", List.of()),
                        perm(55, "系统参数", 1, "system:config", List.of()),
                        perm(56, "接口监控", 1, "system:monitor", List.of()))),
                perm(6, "用户管理", 0, "user", List.of(
                        perm(61, "用户列表", 1, "user:list", List.of()))));
        return Result.ok().add("tree", tree);
    }

    private Map<String, Object> perm(int id, String name, int parentId, String code, List<Map<String, Object>> children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("permName", name);
        m.put("parentId", parentId);
        m.put("permCode", code);
        m.put("type", parentId == 0 ? "menu" : "button");
        if (!children.isEmpty()) {
            m.put("children", children);
        }
        return m;
    }

    // ---------- 用户管理（用户端账号） ----------

    /** 用户列表：按账号/昵称/手机号/用户编号检索，附带订单数与累计消费 */
    public Result mallUserList(Map<String, Object> body) {
        requireAdmin("user:list");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        String keyword = str(body, "keyword");
        Integer status = str(body, "status").isBlank() ? null : intv(body, "status", -1);
        QueryWrapper<MallUser> wrapper = new QueryWrapper<MallUser>()
                .and(!keyword.isBlank(), w -> w.like("username", keyword)
                        .or().like("nickname", keyword)
                        .or().like("phone", keyword)
                        .or().like("user_num", keyword))
                .eq(status != null, "status", status)
                .orderByDesc("create_time");
        Long total = mallUserMapper.selectCount(wrapper);
        List<MallUser> users = mallUserMapper.selectList(wrapper.last("LIMIT " + Math.max(current - 1, 0) * size + "," + size));
        Map<Long, Map<String, Object>> stats = userOrderStats(users.stream().map(MallUser::getId).toList());
        List<Map<String, Object>> list = users.stream().map(u -> {
            Map<String, Object> m = mallUserView(u);
            fillUserStat(m, stats.get(u.getId()));
            return m;
        }).toList();
        return Result.ok().page(total, current, size, list);
    }

    /** 用户详情：基本信息 + 收货地址 + 订单概况 + 最近订单 */
    public Result mallUserDetail(Map<String, Object> body) {
        requireAdmin("user:list");
        long id = longv(body, "id");
        MallUser u = mallUserMapper.selectById(id);
        if (u == null) {
            throw new ApiException("用户不存在");
        }
        Map<String, Object> user = mallUserView(u);
        fillUserStat(user, userOrderStats(List.of(id)).get(id));
        List<Map<String, Object>> addresses = addressMapper.selectList(
                new QueryWrapper<Address>().eq("user_id", id)).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("consignee", a.getConsignee());
            m.put("phone", a.getPhone());
            m.put("full", (a.getProvince() == null ? "" : a.getProvince())
                    + (a.getCity() == null ? "" : a.getCity())
                    + (a.getDistrict() == null ? "" : a.getDistrict())
                    + (a.getDetail() == null ? "" : a.getDetail()));
            m.put("defaultFlag", a.isDefaultFlag());
            return m;
        }).toList();
        List<Map<String, Object>> recentOrders = orderMapper.selectList(new QueryWrapper<Order>()
                        .eq("user_id", id).orderByDesc("create_time").last("LIMIT 10"))
                .stream().map(this::orderView).toList();
        return Result.ok().add("user", user).add("addresses", addresses).add("recentOrders", recentOrders);
    }

    /** 启用 / 禁用用户 */
    public Result mallUserStatus(Map<String, Object> body) {
        AdminUser admin = requireAdmin("user:list");
        MallUser u = mallUserMapper.selectById(longv(body, "id"));
        if (u == null) {
            throw new ApiException("用户不存在");
        }
        int status = intv(body, "status", 1);
        if (status != 0 && status != 1) {
            throw new ApiException("状态参数不合法");
        }
        u.setStatus(status);
        mallUserMapper.updateById(u);
        log(admin, status == 1 ? "启用用户" : "禁用用户", u.getUsername(), "");
        return Result.ok(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /** 重置用户密码 */
    public Result mallUserResetPassword(Map<String, Object> body) {
        AdminUser admin = requireAdmin("user:list");
        MallUser u = mallUserMapper.selectById(longv(body, "id"));
        if (u == null) {
            throw new ApiException("用户不存在");
        }
        String password = str(body, "password");
        if (password.isBlank()) {
            throw new ApiException("新密码不能为空");
        }
        u.setPassword(password);
        mallUserMapper.updateById(u);
        log(admin, "重置用户密码", u.getUsername(), "");
        return Result.ok("密码已重置");
    }

    private Map<String, Object> mallUserView(MallUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("userNum", u.getUserNum());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname() == null ? "" : u.getNickname());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        m.put("phone", u.getPhone() == null ? "" : u.getPhone());
        m.put("status", u.getStatus() == null ? 1 : u.getStatus());
        m.put("createTime", u.getCreateTime());
        return m;
    }

    /** 用户订单统计：订单数 / 有效订单数 / 累计消费 / 最近下单时间 */
    private Map<Long, Map<String, Object>> userOrderStats(List<Long> userIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.select("user_id AS userId",
                        "COUNT(*) AS orderCount",
                        "SUM(CASE WHEN status IN (0,4,5) THEN 1 ELSE 0 END) AS paidCount",
                        "SUM(CASE WHEN status IN (0,4,5) THEN pay_amount ELSE 0 END) AS totalSpent",
                        "MAX(create_time) AS lastOrderTime")
                .in("user_id", userIds)
                .groupBy("user_id");
        for (Map<String, Object> row : orderMapper.selectMaps(wrapper)) {
            Object userId = mapValue(row, "userId", "user_id");
            if (userId == null) {
                continue;
            }
            Map<String, Object> stat = new LinkedHashMap<>();
            Object orderCount = mapValue(row, "orderCount", "order_count");
            Object paidCount = mapValue(row, "paidCount", "paid_count");
            Object totalSpent = mapValue(row, "totalSpent", "total_spent");
            Object lastOrderTime = mapValue(row, "lastOrderTime", "last_order_time");
            stat.put("orderCount", orderCount == null ? 0L : Long.parseLong(String.valueOf(orderCount)));
            stat.put("paidCount", paidCount == null ? 0L : Long.parseLong(String.valueOf(paidCount)));
            stat.put("totalSpent", totalSpent == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(totalSpent)));
            stat.put("lastOrderTime", lastOrderTime == null ? "" : String.valueOf(lastOrderTime));
            result.put(Long.parseLong(String.valueOf(userId)), stat);
        }
        return result;
    }

    private void fillUserStat(Map<String, Object> user, Map<String, Object> stat) {
        user.put("orderCount", stat == null ? 0L : stat.get("orderCount"));
        user.put("paidCount", stat == null ? 0L : stat.get("paidCount"));
        user.put("totalSpent", stat == null ? BigDecimal.ZERO : stat.get("totalSpent"));
        user.put("lastOrderTime", stat == null ? "" : stat.get("lastOrderTime"));
    }

    private Object mapValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public Result operLogList(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:log");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        String keyword = str(body, "keyword");
        QueryWrapper<OperLog> wrapper = new QueryWrapper<OperLog>()
                .and(!keyword.isBlank(), w -> w.like("admin_name", keyword).or().like("action", keyword).or().like("target", keyword))
                .orderByDesc("create_time");
        Long total = operLogMapper.selectCount(wrapper);
        List<Map<String, Object>> list = operLogMapper.selectList(wrapper.last("LIMIT " + (current - 1) * size + "," + size)).stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("adminName", l.getAdminName() == null ? "" : l.getAdminName());
            m.put("action", l.getAction() == null ? "" : l.getAction());
            m.put("target", l.getTarget() == null ? "" : l.getTarget());
            m.put("detail", l.getDetail() == null ? "" : l.getDetail());
            m.put("ip", l.getIp() == null ? "" : l.getIp());
            m.put("createTime", l.getCreateTime());
            return m;
        }).toList();
        return Result.ok().page(total, current, size, list);
    }

    // ---------- 系统运维（5.3.7：系统参数 / 接口监控） ----------

    /** 系统参数列表 */
    public Result sysConfigList(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:config");
        List<Map<String, Object>> list = sysConfigMapper.selectList(new QueryWrapper<>()).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("configKey", c.getConfigKey());
            m.put("configValue", c.getConfigValue() == null ? "" : c.getConfigValue());
            m.put("remark", c.getRemark() == null ? "" : c.getRemark());
            return m;
        }).toList();
        return Result.ok().add("list", list);
    }

    /** 保存系统参数（按 configKey upsert） */
    public Result sysConfigSave(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:config");
        String key = str(body, "configKey");
        if (key.isBlank()) {
            throw new ApiException("参数缺失：configKey");
        }
        SysConfig c = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key));
        if (c == null) {
            c = new SysConfig();
        }
        c.setConfigKey(key);
        c.setConfigValue(str(body, "configValue"));
        c.setRemark(str(body, "remark"));
        if (c.getId() != null) {
            sysConfigMapper.updateById(c);
        } else {
            sysConfigMapper.insert(c);
        }
        log(admin, "系统参数", key, c.getConfigValue());
        return Result.ok("系统参数已保存");
    }

    /** 接口运行状态监控（进程运行时长 / JVM 内存 / 线程 / 接口调用 Top） */
    public Result monitorStatus(Map<String, Object> body) {
        AdminUser admin = requireAdmin("system:monitor");
        return monitorService.status();
    }

    /** 分类/广告管理（运营） */
    public Result categoryTree() {
        AdminUser admin = requireAdmin("advert:list");
        return productService.categoryTree();
    }

    // ---------- 工具 ----------

    /** 支付服务聚合响应（Map）转为统一 Result 返回 */
    private Result wrap(Map<String, Object> res) {
        Result r = new Result();
        if (res != null) {
            r.putAll(res);
        }
        return r;
    }

    private void check(Map<String, Object> res) {
        if (!"0".equals(String.valueOf(res.get("id")))) {
            throw new ApiException(String.valueOf(res.getOrDefault("message", "请求失败")));
        }
    }

    private Map<String, Object> merchantView(Merchant m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("merchantNo", m.getMerchantNo());
        map.put("shopName", m.getShopName());
        map.put("contactName", m.getContactName());
        map.put("shopPhone", m.getShopPhone());
        map.put("shopAddress", m.getShopAddress());
        map.put("categoryName", m.getCategoryName() == null ? "" : m.getCategoryName());
        map.put("status", m.getStatus());
        map.put("rejectReason", m.getRejectReason() == null ? "" : m.getRejectReason());
        map.put("createTime", m.getCreateTime());
        map.put("auditTime", m.getAuditTime() == null ? "" : m.getAuditTime());
        map.put("auditor", m.getAuditor() == null ? "" : m.getAuditor());
        return map;
    }

    private Map<String, Object> orderView(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("merchantNo", o.getMerchantNo());
        m.put("shopName", o.getShopName());
        m.put("buyerName", o.getBuyerName());
        m.put("totalAmount", o.getTotalAmount());
        m.put("payAmount", o.getPayAmount());
        m.put("status", o.getStatus());
        m.put("statusText", orderStatusText(o.getStatus()));
        m.put("channel", o.getChannel() == null ? "" : o.getChannel());
        m.put("payTime", o.getPayTime() == null ? "" : o.getPayTime());
        m.put("createTime", o.getCreateTime());
        m.put("consignee", o.getConsignee());
        m.put("phone", o.getPhone());
        m.put("address", o.getAddress());
        return m;
    }

    private String orderStatusText(int status) {
        return switch (status) {
            case -1 -> "待支付";
            case 0 -> "待发货";
            case 1 -> "支付失败";
            case 2 -> "已退款";
            case 3 -> "已关闭";
            case 4 -> "待收货";
            case 5 -> "已完成";
            default -> "未知";
        };
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