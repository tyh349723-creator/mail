package com.mall.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 时间工具：统一使用 "yyyy-MM-dd HH:mm:ss" 字符串。 */
public final class TimeUtils {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeUtils() {
    }

    public static String now() {
        return LocalDateTime.now().format(DATETIME);
    }

    /** 当前日期字符串 yyyy-MM-dd */
    public static String today() {
        return LocalDate.now().format(DAY);
    }

    public static LocalDateTime parse(String datetime) {
        return LocalDateTime.parse(datetime, DATETIME);
    }

    /** 最近 n 天（含今天）的日期列表，升序 */
    public static java.util.List<String> lastDays(int n) {
        java.util.List<String> days = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = n - 1; i >= 0; i--) {
            days.add(today.minusDays(i).format(DAY));
        }
        return days;
    }
}