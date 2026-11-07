package com.mall.alipay.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.alipay.domain.PayOrder;
import com.mall.alipay.domain.Refund;
import com.mall.alipay.mapper.PayOrderMapper;
import com.mall.alipay.mapper.RefundMapper;
import com.mall.common.constant.StatusConst;
import com.mall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 经营/平台统计：/internal/statis/pay（平台聚合）、/statis/mch（商户经营）。
 * <p>口径：支付成功单为有效交易；金额列二者都返回 交易额 + 手续费（商户端含净额）。</p>
 */
@Service
@RequiredArgsConstructor
public class StatisService {

    private final PayOrderMapper payOrderMapper;
    private final RefundMapper refundMapper;

    /** 有效支付（成功或已退款均累计为交易） */
    private List<PayOrder> validOrders() {
        return payOrderMapper.selectList(new QueryWrapper<PayOrder>()
                .in("status", StatusConst.PAY_SUCCESS, StatusConst.PAY_REFUNDED));
    }

    /** 平台支付统计（全量指标 + 近 7 日趋势） */
    public Result platform() {
        return platform("", "", "day");
    }

    /**
     * 平台支付统计，支持自定义时间范围 begin/end（yyyy-MM-dd）与趋势粒度 type（day/week/month）。
     * begin/end 均空时按全量统计、趋势回退近 7 日；否则按区间过滤并聚合区间趋势。
     */
    public Result platform(String begin, String end, String type) {
        boolean ranged = isRanged(begin, end);
        List<PayOrder> list = ranged
                ? validOrders().stream().filter(p -> inRange(dateOf(p.getPayTime()), begin, end)).toList()
                : validOrders();
        BigDecimal totalMoney = list.stream().map(PayOrder::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> ratio = new LinkedHashMap<>();
        for (PayOrder p : list) {
            ratio.merge(p.getChannelCode(), p.getMoney(), BigDecimal::add);
        }
        List<Map<String, Object>> channelRatio = new ArrayList<>();
        BigDecimal base = totalMoney.signum() == 0 ? BigDecimal.ONE : totalMoney;
        ratio.forEach((code, money) -> channelRatio.add(Map.of(
                "wayCode", code,
                "money", money,
                "ratio", money.multiply(BigDecimal.valueOf(100)).divide(base, 1, java.math.RoundingMode.HALF_UP))));
        Result r = Result.ok("统计成功");
        r.add("begin", begin == null ? "" : begin);
        r.add("end", end == null ? "" : end);
        r.add("totalMoney", totalMoney);
        r.add("totalCount", list.size());
        r.add("channelRatio", channelRatio);
        r.add("sevenDayData", ranged ? rangeTrend(list, begin, end, type) : sevenDay(list));
        r.add("refundCount", refundMapper.selectCount(new QueryWrapper<Refund>().eq("status", StatusConst.REFUND_SUCCESS)));
        r.add("refundMoney", refundMapper.selectList(new QueryWrapper<Refund>().eq("status", StatusConst.REFUND_SUCCESS))
                .stream().map(x -> x.getMoney()).reduce(BigDecimal.ZERO, BigDecimal::add));
        return r;
    }

    /** 商户经营统计（营收/手续费/净收益/渠道占比/7日趋势）——无日期范围重载 */
    public Result merchant(String merchantNo) {
        return merchant(merchantNo, null, null);
    }

    /**
     * 商户经营统计（营收/手续费/净收益/渠道占比/7日趋势，支持自定义时间范围 begin/end，yyyy-MM-dd）。
     * @param begin 起始日期（含），空表示不限制
     * @param end   结束日期（含），空表示不限制
     */
    public Result merchant(String merchantNo, String begin, String end) {
        List<PayOrder> list = validOrders().stream()
                .filter(p -> p.getMerchantNo().equals(merchantNo))
                .filter(p -> inRange(dateOf(p.getPayTime()), begin, end))
                .toList();
        BigDecimal gross = list.stream().map(PayOrder::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee = list.stream().map(PayOrder::getFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> ratio = new LinkedHashMap<>();
        for (PayOrder p : list) {
            ratio.merge(p.getChannelCode(), p.getMoney(), BigDecimal::add);
        }
        List<Map<String, Object>> channelRatio = new ArrayList<>();
        BigDecimal base = gross.signum() == 0 ? BigDecimal.ONE : gross;
        ratio.forEach((code, money) -> channelRatio.add(Map.of(
                "wayCode", code,
                "money", money,
                "ratio", money.multiply(BigDecimal.valueOf(100)).divide(base, 1, java.math.RoundingMode.HALF_UP))));
        // 已退款统计（按审核时间落入区间，口径与交易额一致）
        List<Refund> refunds = refundMapper.selectList(new QueryWrapper<Refund>()
                        .eq("merchant_no", merchantNo).eq("status", StatusConst.REFUND_SUCCESS))
                .stream()
                .filter(x -> inRange(dateOf(x.getAuditTime() == null ? x.getCreateTime() : x.getAuditTime()), begin, end))
                .toList();
        BigDecimal refundMoney = refunds.stream().map(Refund::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        Result r = Result.ok("统计成功");
        r.add("merchantNo", merchantNo);
        r.add("begin", begin == null ? "" : begin);
        r.add("end", end == null ? "" : end);
        r.add("totalMoney", gross);            // 交易额
        r.add("totalCount", list.size());      // 交易笔数
        r.add("feeMoney", fee);                // 手续费
        r.add("netMoney", gross.subtract(fee));// 净收益
        r.add("refundCount", refunds.size());  // 退款笔数
        r.add("refundMoney", refundMoney);     // 退款金额（修复：原实现误填笔数）
        r.add("channelRatio", channelRatio);
        r.add("sevenDayData", sevenDay(list));
        return r;
    }

    /** 取 "yyyy-MM-dd" 部分（为 null/短字符串返回空串） */
    private String dateOf(String t) {
        return t == null || t.length() < 10 ? "" : t.substring(0, 10);
    }

    /** 日期是否落在 [begin, end] 区间（字符串比较；空边界不限制） */
    private boolean inRange(String day, String begin, String end) {
        if (day.isEmpty()) {
            return false;
        }
        boolean afterBegin = begin == null || begin.isBlank() || day.compareTo(begin.trim()) >= 0;
        boolean beforeEnd = end == null || end.isBlank() || day.compareTo(end.trim()) <= 0;
        return afterBegin && beforeEnd;
    }

    /** 近 7 日交易趋势 [{date, money, count}] */
    private List<Map<String, Object>> sevenDay(List<PayOrder> list) {
        List<String> days = PayService.lastSevenDays();
        Map<String, BigDecimal> moneyMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        for (PayOrder p : list) {
            if (p.getPayTime() == null || p.getPayTime().length() < 10) {
                continue;
            }
            String day = p.getPayTime().substring(0, 10);
            moneyMap.merge(day, p.getMoney(), BigDecimal::add);
            countMap.merge(day, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String d : days) {
            result.add(Map.of("date", d,
                    "money", moneyMap.getOrDefault(d, BigDecimal.ZERO),
                    "count", countMap.getOrDefault(d, 0)));
        }
        return result;
    }

    /** begin/end 是否构成有效区间（任一侧非空即视为区间查询） */
    private boolean isRanged(String begin, String end) {
        return (begin != null && !begin.isBlank()) || (end != null && !end.isBlank());
    }

    /** 区间交易趋势：按 type（day/week/month）分桶，区间内空桶补 0 */
    private List<Map<String, Object>> rangeTrend(List<PayOrder> list, String begin, String end, String type) {
        String bucket = type == null ? "day" : type.trim().toLowerCase();
        Map<String, BigDecimal> moneyMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        for (PayOrder p : list) {
            String day = dateOf(p.getPayTime());
            if (day.isEmpty()) {
                continue;
            }
            String key = bucketKey(day, bucket);
            moneyMap.merge(key, p.getMoney(), BigDecimal::add);
            countMap.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : bucketKeys(list, begin, end, bucket)) {
            result.add(Map.of("date", key,
                    "money", moneyMap.getOrDefault(key, BigDecimal.ZERO),
                    "count", countMap.getOrDefault(key, 0)));
        }
        return result;
    }

    /** 日期 → 桶标签：day 原样；week 取所在周周一；month 取 yyyy-MM */
    private String bucketKey(String day, String bucket) {
        return switch (bucket) {
            case "week" -> LocalDate.parse(day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
            case "month" -> day.substring(0, 7);
            default -> day;
        };
    }

    /** 生成区间内连续桶标签；边界缺失或不可解析时退化为按数据日去重 */
    private List<String> bucketKeys(List<PayOrder> list, String begin, String end, String bucket) {
        String start = begin == null ? "" : begin.trim();
        String finish = end == null ? "" : end.trim();
        if (!start.isEmpty() && !finish.isEmpty()) {
            try {
                LocalDate s = LocalDate.parse(start);
                LocalDate e = LocalDate.parse(finish);
                LocalDate cur = switch (bucket) {
                    case "week" -> s.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    case "month" -> s.withDayOfMonth(1);
                    default -> s;
                };
                List<String> keys = new ArrayList<>();
                while (!cur.isAfter(e)) {
                    keys.add(bucketKey(cur.toString(), bucket));
                    cur = switch (bucket) {
                        case "week" -> cur.plusWeeks(1);
                        case "month" -> cur.plusMonths(1);
                        default -> cur.plusDays(1);
                    };
                }
                return keys;
            } catch (Exception ignored) {
                // 非法日期格式：走下方按数据日推导
            }
        }
        Set<String> keys = new LinkedHashSet<>();
        list.stream().map(p -> dateOf(p.getPayTime())).filter(d -> !d.isEmpty()).sorted()
                .forEach(d -> keys.add(bucketKey(d, bucket)));
        return new ArrayList<>(keys);
    }
}