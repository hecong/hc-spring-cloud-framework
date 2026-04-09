package com.hc.framework.mybatis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 配置属性
 *
 * @author hc
 */
@Data
@ConfigurationProperties(prefix = "hc.mybatis-plus")
public class MyBatisPlusProperties {

    /**
     * 是否启用 MyBatis-Plus 自动配置
     */
    private boolean enabled = true;

    /**
     * 分页配置
     */
    private PageConfig page = new PageConfig();

    /**
     * 动态数据源配置
     */
    private DynamicDataSourceConfig dynamicDataSource = new DynamicDataSourceConfig();

    /**
     * 分页配置
     */
    @Data
    public static class PageConfig {
        /**
         * 是否启用分页插件
         */
        private boolean enabled = true;

        /**
         * 分页最大限制
         */
        private Long maxLimit = 1000L;

        /**
         * 溢出总页数后是否进行处理
         */
        private Boolean overflow = true;
    }

    /**
     * 动态数据源配置
     */
    @Data
    public static class DynamicDataSourceConfig {
        /**
         * 是否启用动态数据源
         */
        private boolean enabled = false;

        /**
         * 主数据源名称
         */
        private String primary = "master";

        /**
         * 是否严格模式
         */
        private boolean strict = false;
    }
}
