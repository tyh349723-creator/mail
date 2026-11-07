package com.mall.mgr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mall-X 业务微服务启动类。
 * <p>scanBasePackages 覆盖 com.mall 以便加载 mall-common 的全局异常处理/Redis 工具等共享组件。</p>
 */
@EnableScheduling
@MapperScan("com.mall.mgr.mapper")
@SpringBootApplication(scanBasePackages = "com.mall")
public class MallMgrApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMgrApplication.class, args);
    }
}