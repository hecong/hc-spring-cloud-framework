package com.hc.framework.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.logging")
public class LoggingProperties {

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 是否启用接口日志
     */
    private Boolean apiLogEnabled = true;

    /**
     * 是否启用接口限流
     */
    private Boolean rateLimitEnabled = true;

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 日志忽略路径
     */
    private List<String> ignorePaths = new ArrayList<>();

    /**
     * 限流配置
     */
    @Data
    public static class RateLimitConfig {
        /**
         * 默认QPS限制
         */
        private Double defaultQps = 100.0;

        /**
         * 预热时长（秒）
         */
        private Integer warmUpPeriod = 10;

        /**
         * 是否排队等待
         */
        private Boolean waitEnabled = false;

        /**
         * 最大等待时间（毫秒）
         */
        private Integer maxWaitTime = 500;
    }
}
