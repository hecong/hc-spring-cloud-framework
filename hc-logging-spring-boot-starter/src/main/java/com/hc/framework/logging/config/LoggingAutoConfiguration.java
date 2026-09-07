package com.hc.framework.logging.config;

import com.hc.framework.logging.aspect.ApiLogAspect;
import com.hc.framework.logging.aspect.RateLimiterAspect;
import com.hc.framework.logging.interceptor.FeignTraceIdInterceptor;
import com.hc.framework.logging.interceptor.RestTemplateTraceIdInterceptor;
import com.hc.framework.logging.interceptor.TraceIdInterceptor;
import com.hc.framework.logging.spi.UserIdResolver;
import feign.Feign;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * Logging Starter 自动配置类
 * 提供统一日志 + 接口限流 + 跨服务 TraceId 传递功能
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "hc.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
// 框架默认异步执行器需先于 Boot 的 applicationTaskExecutor 注册，否则会被 Boot 默认执行器抢占（无装饰器）
@AutoConfigureBefore(TaskExecutionAutoConfiguration.class)
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
     * 默认用户ID解析器（返回 null，即匿名用户）
     *
     * <p>业务项目可通过实现 {@link UserIdResolver} 并注册为 Bean 来覆盖此默认实现。</p>
     */
    @Bean
    @ConditionalOnMissingBean(UserIdResolver.class)
    public UserIdResolver userIdResolver() {
        return () -> null;
    }

    /**
     * 限流切面（仅当 classpath 中存在 Sentinel 时才生效）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(com.alibaba.csp.sentinel.SphU.class)
    @ConditionalOnProperty(prefix = "hc.logging.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimiterAspect rateLimiterAspect(LoggingProperties loggingProperties, UserIdResolver userIdResolver) {
        return new RateLimiterAspect(loggingProperties, userIdResolver);
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
            public void addInterceptors(@NotNull InterceptorRegistry registry) {
                registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
            }
        };
    }

    // ==================== 异步 TraceId 透传配置 ====================

    /**
     * 默认 TTL 任务装饰器（业务自定义 TaskDecorator 时自动让位）
     */
    @Bean
    @ConditionalOnMissingBean
    public TtlTaskDecorator ttlTaskDecorator() {
        return new TtlTaskDecorator();
    }

    /**
     * 默认异步执行器：仅当上下文没有任何 {@link Executor} 时注册（与 Boot 默认 executor 同名保证
     * {@code @EnableAsync} 与按名注入的解析确定性），注入 TTL 装饰器实现 TraceId 透传。
     *
     * <p>业务自定义线程池时本 Bean 自动让位，自行 {@code setTaskDecorator(new TtlTaskDecorator())} 接入；
     * 或使用 {@code TraceIdUtils.wrap(...)} 手动包装任务。</p>
     */
    @Bean(name = "applicationTaskExecutor")
    @ConditionalOnMissingBean(Executor.class)
    public ThreadPoolTaskExecutor applicationTaskExecutor(ObjectProvider<TaskDecorator> taskDecorators) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("task-");
        // 优先级选择：业务自定义装饰器 > 框架默认 TTL 装饰器（存在多个装饰器 Bean 时不因歧义失败）
        TaskDecorator decorator = taskDecorators.orderedStream()
                .filter(d -> !(d instanceof TtlTaskDecorator))
                .findFirst()
                .orElseGet(() -> taskDecorators.orderedStream()
                        .filter(TtlTaskDecorator.class::isInstance)
                        .findFirst().orElse(null));
        executor.setTaskDecorator(decorator);
        return executor;
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
