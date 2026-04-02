package com.hc.framework.web.config;

import com.hc.framework.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Web Starter 自动配置类
 * 提供全局统一返回、全局异常处理、参数校验等功能
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "hc.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    /**
     * 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
