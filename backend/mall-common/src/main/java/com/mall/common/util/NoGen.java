package com.mall.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务单号生成器：prefix + yyyyMMddHHmmss + 6位递增序号。
 * 用于商户编号、订单号、支付单号、退款单号、提现单号等。
 */
public final class NoGen {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicLong SEQ = new AtomicLong(0);

    private NoGen() {
    }

    public static String next(String prefix) {
        long seq = SEQ.incrementAndGet() % 1_000_000L;
        return prefix + LocalDateTime.now().format(FMT) + String.format("%06d", seq);
    }
}