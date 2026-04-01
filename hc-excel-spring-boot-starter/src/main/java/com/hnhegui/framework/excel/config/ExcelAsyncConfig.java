package com.hnhegui.framework.excel.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Excel异步执行器配置
 *
 * <p>使用阿里巴巴 TTL (TransmittableThreadLocal) 包装线程池，解决上下文传递问题。</p>
 * <p>支持以下上下文的跨线程传递：</p>
 * <ul>
 *   <li>MDC (Mapped Diagnostic Context) - 日志跟踪ID</li>
 *   <li>RequestContextHolder - Spring 请求上下文</li>
 *   <li>自定义 ThreadLocal 变量</li>
 *   <li>用户登录信息等</li>
 * </ul>
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(ExcelAsyncPoolProperties.class)
public class ExcelAsyncConfig {

    /**
     * Excel异步任务执行器
     *
     * <p>使用 TTL 包装线程池，确保子线程能获取父线程的上下文信息</p>
     */
    @Bean("excelAsyncExecutor")
    public Executor excelAsyncExecutor(ExcelAsyncPoolProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(properties.isAllowCoreThreadTimeOut());
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        // 使用 TTL 包装线程池，支持上下文传递
        return TtlExecutors.getTtlExecutor(executor.getThreadPoolExecutor());
    }
}
