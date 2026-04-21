package com.hc.framework.mybatis.config;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.hc.framework.mybatis.properties.MyBatisPlusProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 动态数据源配置类
 * <p>
 * 基于 baomidou dynamic-datasource-spring-boot-starter 实现
 *
 * @author hc
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DynamicDataSourceContextHolder.class)
@EnableConfigurationProperties(MyBatisPlusProperties.class)
@ConditionalOnProperty(prefix = "hc.mybatis-plus.dynamic-data-source", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DynamicDataSourceConfig {

    /**
     * 动态数据源上下文工具类
     */
    public static class DataSourceContext {

        /**
         * 获取当前数据源
         */
        public static String get() {
            return DynamicDataSourceContextHolder.peek();
        }

        /**
         * 切换数据源
         */
        public static void switchTo(String dataSource) {
            DynamicDataSourceContextHolder.push(dataSource);
            log.debug("切换数据源至: {}", dataSource);
        }

        /**
         * 清除当前数据源
         */
        public static void clear() {
            DynamicDataSourceContextHolder.poll();
        }

        /**
         * 使用指定数据源执行操作
         */
        public static void execute(String dataSource, Runnable task) {
            try {
                switchTo(dataSource);
                task.run();
            } finally {
                clear();
            }
        }
    }
}
