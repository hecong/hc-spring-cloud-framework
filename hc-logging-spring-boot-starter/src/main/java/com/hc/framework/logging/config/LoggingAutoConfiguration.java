package com.hc.framework.logging.config;

import com.hc.framework.logging.aspect.ApiLogAspect;
import com.hc.framework.logging.aspect.RateLimiterAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Logging Starter 自动配置类
 * 提供统一日志 + 接口限流功能
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "hc.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    /**
     * API日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.logging", name = "api-log-enabled", havingValue = "true", matchIfMissing = true)
    public ApiLogAspect apiLogAspect() {
        return new ApiLogAspect();
    }

    /**
     * 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.logging", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }
}
