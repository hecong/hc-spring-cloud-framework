package com.hc.framework.excel.config;

import com.hc.framework.excel.executor.ExcelAsyncExecutor;
import com.hc.framework.excel.service.ExcelExportService;
import com.hc.framework.excel.service.ExcelFileStorage;
import com.hc.framework.excel.service.ExcelImportService;
import com.hc.framework.excel.service.ExcelOperationRecorder;
import com.hc.framework.excel.service.ExcelOperatorResolver;
import com.hc.framework.excel.service.ExcelTaskStore;
import com.hc.framework.excel.service.impl.AnonymousOperatorResolver;
import com.hc.framework.excel.service.impl.ExcelExportServiceImpl;
import com.hc.framework.excel.service.impl.ExcelImportServiceImpl;
import com.hc.framework.excel.service.impl.LocalExcelFileStorage;
import com.hc.framework.excel.service.impl.LocalExcelTaskStore;
import com.hc.framework.excel.service.impl.NoOpExcelOperationRecorder;
import com.hc.framework.excel.service.impl.RedisExcelTaskStore;
import com.hc.framework.excel.util.ExcelStyleUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Excel自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties({ExcelAsyncPoolProperties.class, ExcelTaskStoreProperties.class})
@ConditionalOnProperty(prefix = "hc.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({ExcelAutoConfiguration.RedisTaskStoreConfiguration.class,
         ExcelAutoConfiguration.LocalTaskStoreConfiguration.class,
         ExcelAsyncConfig.class})
public class ExcelAutoConfiguration {

    public ExcelAutoConfiguration(ExcelAsyncPoolProperties poolProperties) {
        ExcelStyleUtil.defaultFontName = poolProperties.getFontName();
    }

    @Bean
    @ConditionalOnMissingBean(ExcelOperationRecorder.class)
    public ExcelOperationRecorder excelOperationRecorder() {
        return new NoOpExcelOperationRecorder();
    }

    @Bean
    @ConditionalOnMissingBean(ExcelFileStorage.class)
    public ExcelFileStorage excelFileStorage() {
        return new LocalExcelFileStorage();
    }

    @Bean
    @ConditionalOnMissingBean(ExcelOperatorResolver.class)
    public ExcelOperatorResolver excelOperatorResolver() {
        return new AnonymousOperatorResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelAsyncExecutor excelAsyncExecutor(ExcelOperationRecorder operationRecorder,
                                                  ExcelFileStorage fileStorage, ExcelOperatorResolver operatorResolver,
                                                  ExcelTaskStore taskStore) {
        return new ExcelAsyncExecutor(operationRecorder, fileStorage, operatorResolver, taskStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelImportService excelImportService(ExcelAsyncExecutor excelAsyncExecutor, ObjectMapper objectMapper) {
        return new ExcelImportServiceImpl(excelAsyncExecutor, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelExportService excelExportService(ExcelAsyncExecutor excelAsyncExecutor) {
        return new ExcelExportServiceImpl(excelAsyncExecutor);
    }

    /**
     * Redis任务存储配置（优先级高于 LocalExcelTaskStore）
     *
     * <p>当项目中引入了 hc-redis-spring-boot-starter 且 RedisTemplate 可用时，
     * 自动使用 Redis 存储任务状态，支持分布式部署。</p>
     */
    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    static class RedisTaskStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(ExcelTaskStore.class)
        @ConditionalOnBean(RedisTemplate.class)
        public ExcelTaskStore redisExcelTaskStore(RedisTemplate<String, Object> redisTemplate,
                                                    ExcelTaskStoreProperties taskStoreProperties) {
            return new RedisExcelTaskStore(redisTemplate, taskStoreProperties);
        }
    }

    /**
     * 本地任务存储配置（兜底，当 Redis 不可用时使用）
     */
    @Configuration
    static class LocalTaskStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(ExcelTaskStore.class)
        public ExcelTaskStore localExcelTaskStore() {
            return new LocalExcelTaskStore();
        }
    }
}
