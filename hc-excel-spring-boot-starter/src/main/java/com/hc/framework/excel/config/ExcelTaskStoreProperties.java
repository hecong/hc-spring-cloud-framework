package com.hc.framework.excel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel 任务存储配置属性
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "hc.excel.task-store")
public class ExcelTaskStoreProperties {

    /**
     * Redis 任务 Key 前缀
     */
    private String taskKeyPrefix = "excel:task:";

    /**
     * Redis 导出文件 Key 前缀
     */
    private String exportKeyPrefix = "excel:export:";

    /**
     * Redis 任务 TTL（小时）
     */
    private long ttlHours = 1;
}
