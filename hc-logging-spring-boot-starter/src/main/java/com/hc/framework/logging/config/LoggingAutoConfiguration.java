package com.hc.framework.logging.config;

import com.hc.framework.logging.aspect.ApiLogAspect;
import com.hc.framework.logging.aspect.RateLimiterAspect;
import com.hc.framework.logging.interceptor.FeignTraceIdInterceptor;
import com.hc.framework.logging.interceptor.RestTemplateTraceIdInterceptor;
import com.hc.framework.logging.interceptor.TraceIdInterceptor;
import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Logging Starter 自动配置类
 * 提供统一日志 + 接口限流 + 跨服务 TraceId 传递功能
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
    public ApiLogAspect apiLogAspect(LoggingProperties loggingProperties) {
        return new ApiLogAspect(loggingProperties);
    }

    /**
     * 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.logging.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimiterAspect rateLimiterAspect(LoggingProperties loggingProperties) {
        return new RateLimiterAspect(loggingProperties);
    }

    /**
     * TraceId 拦截器：提取请求头中的 TraceId 并写入 MDC
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceIdInterceptor traceIdInterceptor() {
        return new TraceIdInterceptor();
    }

    /**
     * 注册 TraceId 拦截器到 MVC，拦截所有请求
     */
    @Bean
    public WebMvcConfigurer traceIdWebMvcConfigurer(TraceIdInterceptor traceIdInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
            }
        };
    }

    // ==================== 跨服务 TraceId 传递配置 ====================

    /**
     * Feign TraceId 拦截器
     * 当 classpath 中存在 Feign 时自动注册，向下游服务传递 X-Trace-Id 请求头
     */
    @Bean
    @ConditionalOnClass(Feign.class)
    @ConditionalOnMissingBean
    public FeignTraceIdInterceptor feignTraceIdInterceptor() {
        return new FeignTraceIdInterceptor();
    }

    /**
     * RestTemplate TraceId 拦截器
     * 当 classpath 中存在 RestTemplate 时自动注册
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnMissingBean
    public RestTemplateTraceIdInterceptor restTemplateTraceIdInterceptor() {
        return new RestTemplateTraceIdInterceptor();
    }

    /**
     * RestTemplate 自定义器：将 TraceId 拦截器自动注入所有 RestTemplate Bean
     * 通过 RestTemplateCustomizer 实现零侵入自动配置
     */
    @Bean
    @ConditionalOnClass(RestTemplate.class)
    public RestTemplateCustomizer traceIdRestTemplateCustomizer(RestTemplateTraceIdInterceptor interceptor) {
        return restTemplate -> restTemplate.getInterceptors().add(interceptor);
    }
}
