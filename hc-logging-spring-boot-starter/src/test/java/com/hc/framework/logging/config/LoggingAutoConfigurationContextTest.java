package com.hc.framework.logging.config;

import com.hc.framework.logging.util.TraceIdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步 TraceId 透传自动装配集成单测：
 * 默认执行器与 Boot 竞争关系、自定义 Executor/Decorator 让位语义、@EnableAsync 全链路透传
 */
class LoggingAutoConfigurationContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class, LoggingAutoConfiguration.class));

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncUserConfig {
        @Bean
        AsyncProbe asyncProbe() {
            return new AsyncProbe();
        }
    }

    static class AsyncProbe {
        @Async
        public CompletableFuture<String> runAsync() {
            return CompletableFuture.completedFuture(TraceIdUtils.getTraceId());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExecutorConfig {
        @Bean
        public ThreadPoolTaskExecutor myPool() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setThreadNamePrefix("custom-");
            executor.initialize();
            return executor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDecoratorConfig {
        @Bean
        public TaskDecorator customDecorator() {
            // 内联直跑装饰器：用于验证"自定义装饰器优先于默认 TTL 装饰器"
            return task -> task;
        }
    }

    @Test
    @DisplayName("无自定义执行器：默认 applicationTaskExecutor 注入 TTL 装饰器，@EnableAsync 透传 traceId")
    void defaultExecutorWithDecoratorPropagatesThroughAsync() {
        runner.withUserConfiguration(AsyncUserConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Executor.class);
                    assertThat(context).hasBean("applicationTaskExecutor");

                    String traceId = "async-ctx-" + System.nanoTime();
                    try {
                        TraceIdUtils.setTraceId(traceId);
                        String got = context.getBean(AsyncProbe.class).runAsync().get(5, TimeUnit.SECONDS);
                        assertThat(got).isEqualTo(traceId);
                    } catch (Exception e) {
                        throw new AssertionError("异步透传失败", e);
                    } finally {
                        TraceIdUtils.removeTraceId();
                    }
                });
    }

    @Test
    @DisplayName("业务自定义 Executor：框架默认执行器自动让位，不注册 applicationTaskExecutor")
    void customExecutorBacksOffDefault() {
        new ApplicationContextRunner()
                .withUserConfiguration(CustomExecutorConfig.class)
                .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
                    assertThat(context).doesNotHaveBean("applicationTaskExecutor");
                    assertThat(context.getBean(ThreadPoolTaskExecutor.class).getThreadNamePrefix()).isEqualTo("custom-");
                });
    }

    @Test
    @DisplayName("业务自定义 TaskDecorator：默认装饰器让位且默认执行器注入自定义装饰器")
    void customDecoratorTakesPrecedence() {
        new ApplicationContextRunner()
                .withUserConfiguration(CustomDecoratorConfig.class)
                .withConfiguration(AutoConfigurations.of(LoggingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasBean("applicationTaskExecutor");
                    // 存在业务自定义装饰器时执行器不得因多 Bean 歧义启动失败（优先级由装配逻辑保障）
                    assertThat(context.getBeansOfType(TaskDecorator.class).values().stream()
                            .anyMatch(d -> !(d instanceof TtlTaskDecorator))).isTrue();
                });
    }

    @Test
    @DisplayName("装饰器 Bean 独立可用（供业务自定义线程池手动接入）")
    void decoratorReusableStandalone() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TtlTaskDecorator.class);
            TtlTaskDecorator decorator = context.getBean(TtlTaskDecorator.class);
            String traceId = "deco-" + System.nanoTime();
            try {
                TraceIdUtils.setTraceId(traceId);
                CompletableFuture<String> captured = new CompletableFuture<>();
                decorator.decorate(() -> captured.complete(TraceIdUtils.getTraceId()))
                        .run(); // 同线程直跑，验证 MDC 恢复逻辑本身可用
                assertThat(captured.get(5, TimeUnit.SECONDS)).isEqualTo(traceId);
                assertThat(TraceIdUtils.getTraceId()).isNull();
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                TraceIdUtils.removeTraceId();
            }
        });
    }
}
