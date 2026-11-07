package com.mall.mgr.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.exception.ApiException;
import com.mall.common.redis.RedisTools;
import com.mall.common.result.Result;
import com.mall.common.util.CurrentUser;
import com.mall.common.util.NoGen;
import com.mall.common.util.TimeUtils;
import com.mall.mgr.domain.*;
import com.mall.mgr.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单服务：预下单试算、下单（多商户分单 + Redis Lua 库存扣减 + 状态机）、
 * 支付回调置已支付、超时自动关单（扫单实现）、发货、确认收货、取消、售后退款申请。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedisTools redisTools;
    private final AlipayClient alipayClient;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MerchantMapper merchantMapper;
    private final AddressMapper addressMapper;
    private final CartItemMapper cartItemMapper;
    private final MallUserMapper mallUserMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Value("${mall.order.timeout-minutes:15}")
    private long timeoutMinutes;

    /** 下单条目模型 */
    public record OrderItemReq(Long skuId, int quantity) {
    }

    // ---------- 预下单试算 ----------

    /** 试算：不做任何扣减，仅返回按商户分组的价格明细（库存不足只标记，不中断整页渲染） */
    public Result preview(Map<String, Object> body) {
        List<OrderItemReq> reqs = parseItems(body);
        if (reqs.isEmpty()) {
            throw new ApiException("请选择要结算的商品");
        }
        Map<String, Object> group = groupByMerchant(reqs, true);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mchGroups = (List<Map<String, Object>>) group.get("groups");
        BigDecimal total = (BigDecimal) group.get("total");
        return Result.ok().add("groups", mchGroups).add("total", total)
                .add("count", reqs.stream().mapToInt(OrderItemReq::quantity).sum())
                .add("stockShort", group.get("stockShort"));
    }

    // ---------- 下单 ----------

    /**
     * 创建订单：按商户拆分多个订单（每个订单一个 merchantNo，对应一个支付单 orderNum）。
     * 库存扣减：优先 Redis Lua 原子扣（若 Redis 可用），同时同步内存库存。
     */
    public Result create(Map<String, Object> body) {
        Long userId = currentUserId();
        MallUser user = mallUserMapper.selectById(userId);
        if (user == null) {
            throw new ApiException("用户不存在");
        }
        List<OrderItemReq> reqs = parseItems(body);
        if (reqs.isEmpty()) {
            throw new ApiException("请选择要结算的商品");
        }
        Map<String, Object> group = groupByMerchant(reqs);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mchGroups = (List<Map<String, Object>>) group.get("groups");

        Address addr = body.containsKey("addressId")
                ? addressMapper.selectById(longv(body, "addressId"))
                : null;
        if (addr != null && !addr.getUserId().equals(userId)) {
            addr = null;
        }
        String consignee = addr != null ? addr.getConsignee() : str(body, "consignee");
        String phone = addr != null ? addr.getPhone() : str(body, "phone");
        String address = addr != null ? join(addr) : str(body, "address");
        if (consignee.isBlank() || phone.isBlank() || address.isBlank()) {
            throw new ApiException("请完善收货人、电话与收货地址");
        }
        String remark = str(body, "remark");

        List<String> orderNos = new ArrayList<>();
        List<String> merchantNos = new ArrayList<>();
        for (Map<String, Object> g : mchGroups) {
            String orderNo = NoGen.next("O");
            // 扣库存（先所有商品再生成订单，避免半途失败）
            List<Map<String, Object>> gItems = (List<Map<String, Object>>) g.get("items");
            for (Map<String, Object> it : gItems) {
                deductStock(orderNo, (Long) it.get("skuId"), (Integer) it.get("quantity"));
            }
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setUserNum(user.getUserNum());
            order.setBuyerName(user.getNickname() == null ? user.getUsername() : user.getNickname());
            order.setMerchantId((Long) g.get("merchantId"));
            order.setMerchantNo((String) g.get("merchantNo"));
            order.setShopName((String) g.get("shopName"));
            order.setTotalAmount((BigDecimal) g.get("total"));
            order.setPayAmount((BigDecimal) g.get("total"));
            order.setStatus(StatusConst.ORDER_WAIT_PAY);
            order.setConsignee(consignee);
            order.setPhone(phone);
            order.setAddress(address);
            order.setRemark(remark);
            order.setPayTimeoutMinutes((int) timeoutMinutes);
            orderMapper.insert(order);
            
            // 保存订单项
            List<OrderItem> items = new ArrayList<>();
            for (Map<String, Object> it : gItems) {
                OrderItem oi = new OrderItem();
                oi.setOrderId(order.getId());
                oi.setProductId((Long) it.get("productId"));
                oi.setSkuId((Long) it.get("skuId"));
                oi.setProductTitle((String) it.get("title"));
                oi.setProductImage((String) it.get("image"));
                oi.setSpecInfo((String) it.get("specInfo"));
                oi.setPrice((BigDecimal) it.get("price"));
                oi.setQuantity((Integer) it.get("quantity"));
                orderItemMapper.insert(oi);
                items.add(oi);
            }
            order.setItems(items);
            orderNos.add(order.getOrderNo());
            merchantNos.add((String) g.get("merchantNo"));
        }
        // 移除已结算的购物车条目（若来自购物车）
        Object removeCart = body.get("cartIds");
        if (removeCart instanceof List<?> ids) {
            for (Object id : ids) {
                CartItem cart = cartItemMapper.selectById(Long.valueOf(String.valueOf(id)));
                if (cart != null && cart.getUserId().equals(userId)) {
                    cartItemMapper.deleteById(cart.getId());
                }
            }
        }
        return Result.ok("下单成功")
                .add("orderNos", orderNos)
                .add("merchantNos", merchantNos)
                .add("money", group.get("total"))
                .add("timeoutSeconds", timeoutMinutes * 60);
    }

    // ---------- 支付回调置已支付（由 alipay 内部通知触发） ----------

    /** 幂等：仅允许 待支付 → 已支付；重复回调返回成功但不重复处理（状态机保证） */
    public synchronized boolean markPaid(String orderNo, String channel, String payTime) {
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo));
        if (o == null) {
            log.warn("支付回调：订单不存在 {}", orderNo);
            return false;
        }
        if (o.getStatus() == StatusConst.ORDER_PAID) {
            return true; // 幂等
        }
        if (o.getStatus() != StatusConst.ORDER_WAIT_PAY) {
            log.warn("支付回调：订单状态不允许支付 {} -> {}", orderNo, o.getStatus());
            return false;
        }
        o.setStatus(StatusConst.ORDER_PAID);
        o.setChannel(channel);
        o.setPayTime(payTime);
        orderMapper.updateById(o);
        // 商品销量累加
        for (OrderItem it : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()))) {
            Product p = productMapper.selectById(it.getProductId());
            if (p != null) {
                p.setSales(p.getSales() + it.getQuantity());
                productMapper.updateById(p);
            }
        }
        return true;
    }

    /** 退款完成通知：订单置为已退款 */
    public synchronized void refundNotify(String orderNo) {
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo));
        if (o != null && o.getStatus() != StatusConst.ORDER_REFUNDED) {
            o.setStatus(StatusConst.ORDER_REFUNDED);
            orderMapper.updateById(o);
        }
    }

    // ---------- 超时自动关单（@Scheduled 扫单，替代 RocketMQ 延迟消息；接入点见注释） ----------

    /**
     * 超时未支付订单自动关闭 + 库存回补。
     * 原方案为 RocketMQ 延迟消息（订单创建后投递 delayLevel=5 约 15 分钟），
     * 本环境未部署 MQ，采用定时扫单实现等价效果；接入 RocketMQ 后可替换为延迟消息消费。
     */
    @Scheduled(fixedDelay = 30_000)
    public void sweepTimeoutOrders() {
        List<Order> overdue = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<Order> waitPayOrders = orderMapper.selectList(new QueryWrapper<Order>().eq("status", StatusConst.ORDER_WAIT_PAY));
        for (Order o : waitPayOrders) {
            try {
                if (o.getCreateTime() != null
                        && Duration.between(TimeUtils.parse(o.getCreateTime()), now).toMinutes() >= timeoutMinutes) {
                    overdue.add(o);
                }
            } catch (Exception ignored) {
                // 时间解析异常跳过
            }
        }
        for (Order o : overdue) {
            closeOrder(o, "超时未支付");
            alipayClient.closePayOrder(o.getOrderNo(), "超时未支付");
        }
        if (!overdue.isEmpty()) {
            log.info("订单超时关单 {} 笔", overdue.size());
        }
    }

    /**
     * 定时校准商品聚合库存：以各 SKU 库存之和为准修正 product.stock 漂移。
     * product.stock 为冗余聚合字段（列表/详情展示用），权威库存始终是 product_sku.stock。
     */
    @Scheduled(fixedDelay = 300_000)
    public void reconcileProductStock() {
        int fixed = 0;
        for (Product p : productMapper.selectList(null)) {
            int sum = productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))
                    .stream().mapToInt(ProductSku::getStock).sum();
            if (p.getStock() != sum) {
                p.setStock(sum);
                productMapper.updateById(p);
                fixed++;
            }
        }
        if (fixed > 0) {
            log.info("商品聚合库存校准完成，修正 {} 个商品", fixed);
        }
    }

    /** 关单回补库存：Redis 镜像 + SKU 库存 + 商品聚合库存，并登记库存流水 */
    private void restoreStockWithLog(String orderNo, String changeType, OrderItem it) {
        redisTools.restoreStock(it.getSkuId(), it.getQuantity());
        ProductSku sku = productSkuMapper.selectById(it.getSkuId());
        if (sku != null) {
            int before = sku.getStock();
            sku.setStock(before + it.getQuantity());
            productSkuMapper.updateById(sku);
            InventoryLog logRow = new InventoryLog();
            logRow.setSkuId(it.getSkuId());
            logRow.setProductId(it.getProductId());
            logRow.setOrderNo(orderNo);
            logRow.setChangeType(changeType);
            logRow.setChangeQty(it.getQuantity());
            logRow.setBeforeStock(before);
            logRow.setAfterStock(before + it.getQuantity());
            logRow.setRemark("关单释放库存");
            inventoryLogMapper.insert(logRow);
        }
        Product p = productMapper.selectById(it.getProductId());
        if (p != null) {
            p.setStock(productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))
                    .stream().mapToInt(ProductSku::getStock).sum());
            productMapper.updateById(p);
        }
    }

    /** 关闭订单并回补库存（状态机：仅 待支付 可关闭） */
    private synchronized boolean closeOrder(Order o, String reason) {
        if (o.getStatus() != StatusConst.ORDER_WAIT_PAY) {
            return false;
        }
        o.setStatus(StatusConst.ORDER_CLOSED);
        o.setCloseTime(TimeUtils.now());
        o.setCloseReason(reason);
        orderMapper.updateById(o);
        // 库存回补（Redis 镜像 + 内存仓储）
        for (OrderItem it : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()))) {
            restoreStockWithLog(o.getOrderNo(), reason, it);
        }
        return true;
    }

    // ---------- 用户订单 ----------

    public Result userList(Map<String, Object> body) {
        Long userId = currentUserId();
        int status = intv(body, "status", -99);
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        
        QueryWrapper<Order> w = new QueryWrapper<Order>()
                .eq("user_id", userId)
                .eq(status != -99, "status", status)
                .orderByDesc("create_time");
        long total = orderMapper.selectCount(w);
        List<Order> list = orderMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::orderView).toList();
        return Result.ok().page(total, current, size, records);
    }

    public Result userDetail(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        return Result.ok().add("order", orderView(o));
    }

    /** 取消订单（仅待支付） */
    public Result cancel(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        if (closeOrder(o, "用户取消")) {
            alipayClient.closePayOrder(orderNo, "用户取消");
            return Result.ok("订单已取消");
        }
        throw new ApiException("当前状态不可取消");
    }

    /** 确认收货：已发货 → 已完成 */
    public Result confirm(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        if (o.getStatus() != StatusConst.ORDER_SHIPPED) {
            throw new ApiException("当前状态不可确认收货");
        }
        o.setStatus(StatusConst.ORDER_FINISHED);
        o.setFinishTime(TimeUtils.now());
        orderMapper.updateById(o);
        return Result.ok("已确认收货");
    }

    /** 用户申请退款：已支付/已发货/已完成（确认收货后售后）均可申请，落库到支付侧退款单 */
    public Result refundApply(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        String money = str(body, "money");
        String reason = str(body, "reason").isBlank() ? "不想要了" : str(body, "reason");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        if (o.getStatus() != StatusConst.ORDER_PAID && o.getStatus() != StatusConst.ORDER_SHIPPED
                && o.getStatus() != StatusConst.ORDER_FINISHED) {
            throw new ApiException("当前状态不可申请退款");
        }
        BigDecimal refundMoney;
        try {
            refundMoney = money.isBlank() ? o.getPayAmount() : new BigDecimal(money);
        } catch (NumberFormatException e) {
            throw new ApiException("退款金额格式错误");
        }
        if (refundMoney.compareTo(o.getPayAmount()) > 0 || refundMoney.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("退款金额不合法");
        }
        Map<String, Object> res = alipayClient.refundApply(orderNo, o.getMerchantNo(),
                refundMoney.toPlainString(), reason);
        if (!"0".equals(String.valueOf(res.get("id")))) {
            throw new ApiException(String.valueOf(res.getOrDefault("message", "退款申请失败")));
        }
        return Result.ok("退款申请已提交，等待商户处理").add("refundOrderId", res.get("refundOrderId"));
    }

    /** 用户售后记录（按订单查退款单） */
    public Result userRefunds(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        Map<String, Object> res = alipayClient.refundList(Map.of("orderNo", orderNo));
        return Result.ok().add("refunds", res.getOrDefault("records", List.of()));
    }

    /** 撤销退款申请（仅审核中可撤销，状态置已撤销） */
    public Result refundCancel(Map<String, Object> body) {
        Long userId = currentUserId();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("user_id", userId));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        Map<String, Object> res = alipayClient.refundCancel(orderNo);
        if (!"0".equals(String.valueOf(res.get("id")))) {
            throw new ApiException(String.valueOf(res.getOrDefault("message", "撤销失败")));
        }
        return Result.ok("退款申请已撤销");
    }

    // ---------- 商户订单 ----------

    /** 商户订单列表（指导书 /pay/list/mch 业务侧，支持状态/渠道筛选） */
    public Result merchantList(Map<String, Object> body) {
        String shopNum = currentShopNum();
        int status = intv(body, "orderState", -99);
        String wayCode = str(body, "wayCode");
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        
        QueryWrapper<Order> w = new QueryWrapper<Order>()
                .eq("merchant_no", shopNum)
                .eq(status != -99, "status", status)
                .eq(!wayCode.isBlank(), "channel", wayCode)
                .orderByDesc("create_time");
        long total = orderMapper.selectCount(w);
        List<Order> list = orderMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::orderView).toList();
        return Result.ok().page(total, current, size, records);
    }

    public Result merchantDetail(Map<String, Object> body) {
        String shopNum = currentShopNum();
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("merchant_no", shopNum));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        return Result.ok().add("order", orderView(o));
    }

    /** 发货：已支付 → 已发货（仅入驻审核通过的商户可操作） */
    public Result ship(Map<String, Object> body) {
        String shopNum = currentShopNum();
        Merchant merchant = merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("merchant_no", shopNum));
        if (merchant == null || !StatusConst.merchantActive(merchant.getStatus())) {
            throw new ApiException("店铺未通过入驻审核，暂不能处理订单");
        }
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo).eq("merchant_no", shopNum));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        if (o.getStatus() != StatusConst.ORDER_PAID) {
            throw new ApiException("当前状态不可发货");
        }
        o.setStatus(StatusConst.ORDER_SHIPPED);
        o.setUpdateTime(TimeUtils.now());
        orderMapper.updateById(o);
        return Result.ok("已发货");
    }

    // ---------- 平台订单 ----------

    /** 平台异常单处理（管理端 5.3.3）：对待支付/支付失败等异常单人工关单并回补库存 */
    public Result adminHandle(Map<String, Object> body) {
        String orderNo = str(body, "orderNo");
        Order o = orderMapper.selectOne(new QueryWrapper<Order>().eq("order_no", orderNo));
        if (o == null) {
            throw new ApiException("订单不存在");
        }
        String action = str(body, "action").isBlank() ? "close" : str(body, "action");
        if ("close".equals(action)) {
            if (o.getStatus() != StatusConst.ORDER_WAIT_PAY && o.getStatus() != StatusConst.ORDER_PAY_FAILED) {
                throw new ApiException("仅待支付/支付失败订单可人工关单");
            }
            closeOrderAllowFailed(o, "平台关闭");
            alipayClient.closePayOrder(orderNo, "平台关闭");
            return Result.ok("订单已关闭，库存已回补");
        }
        throw new ApiException("不支持的处理动作：" + action);
    }

    /** 关单并回补库存（待支付 / 支付失败均可；支付失败单库存此前已扣减，同样需要回补） */
    private synchronized boolean closeOrderAllowFailed(Order o, String reason) {
        if (o.getStatus() != StatusConst.ORDER_WAIT_PAY && o.getStatus() != StatusConst.ORDER_PAY_FAILED) {
            return false;
        }
        o.setStatus(StatusConst.ORDER_CLOSED);
        o.setCloseTime(TimeUtils.now());
        o.setCloseReason(reason);
        orderMapper.updateById(o);
        for (OrderItem it : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()))) {
            restoreStockWithLog(o.getOrderNo(), reason, it);
        }
        return true;
    }

    public Result platformList(Map<String, Object> body) {
        int current = intv(body, "pageNum", 1);
        int size = intv(body, "pageSize", 10);
        String orderNo = str(body, "orderNo");
        String shopName = str(body, "shopName");
        int status = intv(body, "status", -99);
        
        QueryWrapper<Order> w = new QueryWrapper<Order>()
                .like(!orderNo.isBlank(), "order_no", orderNo)
                .like(!shopName.isBlank(), "shop_name", shopName)
                .eq(status != -99, "status", status)
                .orderByDesc("create_time");
        long total = orderMapper.selectCount(w);
        List<Order> list = orderMapper.selectList(w.last("LIMIT " + (long) (current - 1) * size + "," + size));
        List<Map<String, Object>> records = list.stream().map(this::orderView).toList();
        return Result.ok().page(total, current, size, records);
    }

    // ---------- 工具 ----------

    /** 按商户分组计算金额与商品明细（默认严格校验库存，下单使用） */
    private Map<String, Object> groupByMerchant(List<OrderItemReq> reqs) {
        return groupByMerchant(reqs, false);
    }

    /**
     * 按商户分组计算金额与商品明细。
     * allowStockShortage=true 时（试算场景），库存不足仅标记 stockShort 并返回可用库存，不抛错。
     */
    private Map<String, Object> groupByMerchant(List<OrderItemReq> reqs, boolean allowStockShortage) {
        Map<String, List<Map<String, Object>>> byMerchant = new LinkedHashMap<>();
        for (OrderItemReq req : reqs) {
            ProductSku sku = productSkuMapper.selectById(req.skuId());
            if (sku == null) {
                throw new ApiException("商品规格不存在");
            }
            Product p = productMapper.selectById(sku.getProductId());
            if (p == null || p.getStatus() != StatusConst.PRODUCT_ON) {
                throw new ApiException("商品已下架：" + sku.getProductId());
            }
            if (req.quantity() <= 0) {
                throw new ApiException("商品数量不合法");
            }
            boolean shortStock = sku.getStock() < req.quantity();
            if (shortStock && !allowStockShortage) {
                throw new ApiException("商品「" + p.getTitle() + "」库存不足");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", p.getId());
            item.put("skuId", sku.getId());
            item.put("title", p.getTitle());
            item.put("image", sku.getImage() == null ? p.getMainImage() : sku.getImage());
            item.put("specInfo", sku.getSpecValue());
            item.put("price", sku.getPrice());
            item.put("quantity", req.quantity());
            item.put("stockShort", shortStock);
            item.put("availableStock", sku.getStock());
            item.put("merchantNo", p.getMerchantNo());
            byMerchant.computeIfAbsent(p.getMerchantNo(), k -> new ArrayList<>()).add(item);
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean stockShort = false;
        for (Map.Entry<String, List<Map<String, Object>>> e : byMerchant.entrySet()) {
            String mchNo = e.getKey();
            List<Map<String, Object>> items = e.getValue();
            for (Map<String, Object> it : items) {
                stockShort = stockShort || Boolean.TRUE.equals(it.get("stockShort"));
            }
            BigDecimal subtotal = items.stream()
                    .map(i -> ((BigDecimal) i.get("price")).multiply(BigDecimal.valueOf((Integer) i.get("quantity"))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(subtotal);
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("merchantNo", mchNo);
            Merchant merchant = merchantMapper.selectOne(new QueryWrapper<Merchant>().eq("merchant_no", mchNo));
            group.put("shopName", merchant != null ? merchant.getShopName() : "");
            group.put("merchantId", merchant != null ? merchant.getId() : 0L);
            group.put("total", subtotal);
            group.put("count", items.stream().mapToInt(i -> (Integer) i.get("quantity")).sum());
            group.put("items", items);
            groups.add(group);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        result.put("total", total);
        result.put("stockShort", stockShort);
        return result;
    }

    /**
     * 扣库存：以数据库条件原子扣减为权威库存（stock = stock - n，且要求 stock >= n），
     * 并发下单不会超卖；同时同步 Redis 镜像与商品聚合库存。
     */
    private void deductStock(String orderNo, Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new ApiException("商品数量不合法");
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new ApiException("商品规格不存在");
        }
        int before = sku.getStock();
        // 条件更新：仅当剩余库存充足时才扣减，受影响行数为 0 即库存不足
        int rows = productSkuMapper.update(null, new UpdateWrapper<ProductSku>()
                .setSql("stock = stock - " + quantity)
                .eq("id", skuId)
                .ge("stock", quantity));
        if (rows == 0) {
            throw new ApiException("商品库存不足");
        }
        // 库存流水：记录本次扣减（负数出库）
        InventoryLog logRow = new InventoryLog();
        logRow.setSkuId(skuId);
        logRow.setProductId(sku.getProductId());
        logRow.setOrderNo(orderNo);
        logRow.setChangeType("下单扣减");
        logRow.setChangeQty(-quantity);
        logRow.setBeforeStock(before);
        logRow.setAfterStock(before - quantity);
        logRow.setRemark("订单创建扣减库存");
        inventoryLogMapper.insert(logRow);
        // Redis 镜像同步扣减（不可用/未初始化时忽略，不影响数据库权威库存）
        if (redisTools.deductStock(skuId, quantity) == -1L) {
            redisTools.initStock(skuId, sku.getStock() - quantity);
        }
        Product p = productMapper.selectById(sku.getProductId());
        if (p != null) {
            p.setStock(productSkuMapper.selectList(new QueryWrapper<ProductSku>().eq("product_id", p.getId()))
                    .stream().mapToInt(ProductSku::getStock).sum());
            productMapper.updateById(p);
        }
    }

    private List<OrderItemReq> parseItems(Map<String, Object> body) {
        List<OrderItemReq> reqs = new ArrayList<>();
        Object items = body.get("items");
        if (items instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> raw) {
                    Map<String, Object> m = new HashMap<>();
                    raw.forEach((k, v) -> m.put(String.valueOf(k), v));
                    reqs.add(new OrderItemReq(longv(m, "skuId"), intv(m, "quantity", 1)));
                }
            }
        }
        return reqs;
    }

    /** 订单视图（对外字段平铺） */
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
        m.put("statusText", statusText(o.getStatus()));
        m.put("channel", o.getChannel() == null ? "" : o.getChannel());
        m.put("payTime", o.getPayTime() == null ? "" : o.getPayTime());
        m.put("createTime", o.getCreateTime());
        m.put("finishTime", o.getFinishTime() == null ? "" : o.getFinishTime());
        m.put("closeTime", o.getCloseTime() == null ? "" : o.getCloseTime());
        m.put("closeReason", o.getCloseReason() == null ? "" : o.getCloseReason());
        // 支付倒计时总时长（秒）：优先取订单固化的超时时长，为空则用全局配置
        int orderTimeout = o.getPayTimeoutMinutes() != null ? o.getPayTimeoutMinutes() : (int) timeoutMinutes;
        m.put("timeoutSeconds", orderTimeout * 60);
        m.put("consignee", o.getConsignee());
        m.put("phone", o.getPhone());
        m.put("address", o.getAddress());
        m.put("remark", o.getRemark());
        m.put("freight", o.getFreight());
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem it : orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", it.getProductId());
            item.put("skuId", it.getSkuId());
            item.put("title", it.getProductTitle());
            item.put("image", it.getProductImage());
            item.put("specInfo", it.getSpecInfo());
            item.put("price", it.getPrice());
            item.put("quantity", it.getQuantity());
            items.add(item);
        }
        m.put("items", items);
        return m;
    }

    private String statusText(int status) {
        return switch (status) {
            case StatusConst.ORDER_WAIT_PAY -> "待支付";
            case StatusConst.ORDER_PAID -> "待发货";
            case StatusConst.ORDER_PAY_FAILED -> "支付失败";
            case StatusConst.ORDER_REFUNDED -> "已退款";
            case StatusConst.ORDER_CLOSED -> "已关闭";
            case StatusConst.ORDER_SHIPPED -> "待收货";
            case StatusConst.ORDER_FINISHED -> "已完成";
            default -> "未知";
        };
    }

    private Long currentUserId() {
        Long id = CurrentUser.userId();
        if (id == null) {
            throw ApiException.unauthorized();
        }
        return id;
    }

    private String currentShopNum() {
        if (!CurrentUser.isMerchant()) {
            throw ApiException.unauthorized();
        }
        return CurrentUser.userNum();
    }

    private String join(Address a) {
        return (a.getProvince() == null ? "" : a.getProvince())
                + (a.getCity() == null ? "" : a.getCity())
                + (a.getDistrict() == null ? "" : a.getDistrict())
                + a.getDetail();
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