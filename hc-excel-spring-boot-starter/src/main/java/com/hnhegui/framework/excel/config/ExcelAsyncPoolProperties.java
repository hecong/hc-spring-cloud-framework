package com.hnhegui.framework.excel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel异步线程池配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.excel.async-pool")
public class ExcelAsyncPoolProperties {

    /**
     * 核心线程数
     */
    private int corePoolSize = 4;

    /**
     * 最大线程数
     */
    private int maxPoolSize = 8;

    /**
     * 队列容量
     */
    private int queueCapacity = 100;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "excel-async-";

    /**
     * 允许核心线程超时
     */
    private boolean allowCoreThreadTimeOut = true;

    /**
     * 空闲线程存活时间（秒）
     */
    private int keepAliveSeconds = 60;
}
