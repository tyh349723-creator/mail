package com.mall.alipay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付渠道微服务 mall-alipay（引导书三服务之一，端口 10012）。
 * <p>职责：支付渠道参数配置（基础费率/商户差异化费率/开关）、统一支付单 /pay/create、
 * 模拟渠道收银台与异步回调（幂等：Redis 锁 + 内存唯一标识 + 状态机）、退款、商户钱包/提现、经营统计。
 * 数据内存存储（无数据库），接入 MySQL 时替换仓储实现。</p>
 */
@MapperScan("com.mall.alipay.mapper")
@SpringBootApplication(scanBasePackages = "com.mall")
public class MallAlipayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAlipayApplication.class, args);
    }
}