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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Excel自动配置类
 */
@Configuration
@EnableConfigurationProperties(ExcelAsyncPoolProperties.class)
@Import(ExcelAutoConfiguration.RedisTaskStoreConfiguration.class)
public class ExcelAutoConfiguration {

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
    @ConditionalOnMissingBean(ExcelTaskStore.class)
    public ExcelTaskStore localExcelTaskStore() {
        return new LocalExcelTaskStore();
    }

    @Bean
    public ExcelAsyncExecutor excelAsyncExecutor(ExcelOperationRecorder operationRecorder,
                                                  ExcelFileStorage fileStorage, ExcelOperatorResolver operatorResolver,
                                                  ExcelTaskStore taskStore) {
        return new ExcelAsyncExecutor(operationRecorder, fileStorage, operatorResolver, taskStore);
    }

    @Bean
    public ExcelImportService excelImportService(ExcelAsyncExecutor excelAsyncExecutor) {
        return new ExcelImportServiceImpl(excelAsyncExecutor);
    }

    @Bean
    public ExcelExportService excelExportService(ExcelAsyncExecutor excelAsyncExecutor) {
        return new ExcelExportServiceImpl(excelAsyncExecutor);
    }

    /**
     * Redis任务存储配置（仅在classpath中存在RedisTemplate时激活）
     *
     * <p>优先级高于LocalExcelTaskStore：当项目中引入了hc-redis-spring-boot-starter时，
     * 自动使用Redis存储任务状态，支持分布式部署。</p>
     */
    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    static class RedisTaskStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(ExcelTaskStore.class)
        @ConditionalOnBean(RedisTemplate.class)
        public ExcelTaskStore redisExcelTaskStore(RedisTemplate<String, Object> redisTemplate) {
            return new RedisExcelTaskStore(redisTemplate);
        }
    }
}
