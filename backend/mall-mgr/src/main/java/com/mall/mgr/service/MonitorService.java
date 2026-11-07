package com.mall.mgr.service;

import com.mall.common.result.Result;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口运行状态监控（指导书 5.3.7 系统运维-接口运行状态）：
 * 进程启动时长 / 启动时间 / JVM 堆内存 / 线程数 / 累计调用次数 / 接口调用 Top10。
 * 调用计数由 {@link com.mall.mgr.config.ApiMonitorFilter} 在每个业务请求进入时递增。
 */
@Service
public class MonitorService {

    /** 接口调用计数（请求路径 → 次数） */
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    private final long startTimeMillis = System.currentTimeMillis();

    /** 接口调用计数 +1 */
    public void record(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        counters.computeIfAbsent(path, k -> new AtomicLong()).incrementAndGet();
    }

    /** 采集运行状态 */
    public Result status() {
        Runtime rt = Runtime.getRuntime();
        long heapUsedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long heapTotalMb = rt.totalMemory() / 1024 / 1024;
        long heapMaxMb = rt.maxMemory() / 1024 / 1024;
        int threads = ManagementFactory.getThreadMXBean().getThreadCount();
        long totalCalls = counters.values().stream().mapToLong(AtomicLong::get).sum();
        List<Map<String, Object>> top = counters.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue(Comparator.comparingLong(AtomicLong::get)).reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("path", e.getKey());
                    m.put("count", e.getValue().get());
                    return m;
                })
                .toList();

        Result r = Result.ok("监控数据采集成功");
        String startTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault()));
        r.add("uptimeSeconds", (System.currentTimeMillis() - startTimeMillis) / 1000);
        r.add("startTime", startTime);
        r.add("jvm", Map.of("heapUsedMb", heapUsedMb, "heapTotalMb", heapTotalMb, "heapMaxMb", heapMaxMb));
        r.add("threads", threads);
        r.add("totalCalls", totalCalls);
        r.add("top", top);
        return r;
    }
}