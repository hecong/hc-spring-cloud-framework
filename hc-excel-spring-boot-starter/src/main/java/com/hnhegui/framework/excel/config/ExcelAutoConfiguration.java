package com.hnhegui.framework.excel.config;

import com.hnhegui.framework.excel.executor.ExcelAsyncExecutor;
import com.hnhegui.framework.excel.service.ExcelExportService;
import com.hnhegui.framework.excel.service.ExcelFileStorage;
import com.hnhegui.framework.excel.service.ExcelImportService;
import com.hnhegui.framework.excel.service.ExcelOperationRecorder;
import com.hnhegui.framework.excel.service.ExcelOperatorResolver;
import com.hnhegui.framework.excel.service.impl.AnonymousOperatorResolver;
import com.hnhegui.framework.excel.service.impl.ExcelExportServiceImpl;
import com.hnhegui.framework.excel.service.impl.ExcelImportServiceImpl;
import com.hnhegui.framework.excel.service.impl.LocalExcelFileStorage;
import com.hnhegui.framework.excel.service.impl.NoOpExcelOperationRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Excel自动配置类
 */
@Configuration
@EnableConfigurationProperties(ExcelAsyncPoolProperties.class)
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
    public ExcelAsyncExecutor excelAsyncExecutor(ExcelOperationRecorder operationRecorder,
                                                  ExcelFileStorage fileStorage, ExcelOperatorResolver operatorResolver) {
        return new ExcelAsyncExecutor(operationRecorder, fileStorage, operatorResolver);
    }

    @Bean
    public ExcelImportService excelImportService(ExcelAsyncExecutor excelAsyncExecutor) {
        return new ExcelImportServiceImpl(excelAsyncExecutor);
    }

    @Bean
    public ExcelExportService excelExportService(ExcelAsyncExecutor excelAsyncExecutor) {
        return new ExcelExportServiceImpl(excelAsyncExecutor);
    }
}
