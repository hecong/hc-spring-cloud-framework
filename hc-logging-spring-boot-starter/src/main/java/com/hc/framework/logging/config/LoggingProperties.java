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
     * 日志忽略路径
     */
    private List<String> ignorePaths = new ArrayList<>();

    /**
     * 敏感参数名列表（匹配时忽略大小写，值将被替换为 ***）
     */
    private List<String> sensitiveParamNames = List.of(
            "password", "secret", "token", "idCard", "accessToken", "refreshToken",
            "creditCard", "cvv", "privateKey"
    );

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * API 日志采样与序列化体积配置
     */
    private ApiLogConfig apiLog = new ApiLogConfig();

    /**
     * 限流嵌套配置
     */
    @Data
    public static class RateLimitConfig {
        /**
         * 是否启用接口限流
         */
        private Boolean enabled = true;

        /**
         * 默认QPS限制
         */
        private Double defaultQps = 100.0;

        /**
         * 预热时长（秒）
         */
        private Integer warmUpPeriod = 10;

        /**
         * 最大等待时间（毫秒）
         */
        private Integer maxWaitTime = 500;

        /**
         * 是否排队等待
         */
        private Boolean waitEnabled = false;
    }

    /**
     * API 日志嵌套配置
     */
    @Data
    public static class ApiLogConfig {
        /**
         * 采样率（0.0-1.0，默认 1.0 全量）；采样按 traceId 确定性取模，同一链路判定一致；异常日志不受采样控制
         */
        private Double sampleRate = 1.0;

        /**
         * 序列化长度上限（字符），默认 4096；超限仅记录 &lt;truncated, len=N&gt; 截断标记
         */
        private Integer maxSerializeLength = 4096;
    }
}