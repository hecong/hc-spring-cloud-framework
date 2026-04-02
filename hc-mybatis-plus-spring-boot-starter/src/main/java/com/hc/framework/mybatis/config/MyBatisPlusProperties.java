package com.hc.framework.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis-Plus配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.mybatis-plus")
public class MyBatisPlusProperties {

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 是否启用多数据源
     */
    private Boolean multiDatasource = false;

    /**
     * 主数据源
     */
    private String primary = "master";

    /**
     * 数据源配置列表
     */
    private List<DatasourceConfig> datasources = new ArrayList<>();

    /**
     * 全局配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 数据源配置
     */
    @Data
    public static class DatasourceConfig {
        private String name;
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String type = "com.alibaba.druid.pool.DruidDataSource";
    }

    /**
     * 全局配置
     */
    @Data
    public static class GlobalConfig {
        private Boolean banner = false;
        private String dbConfigIdType = "assign_id";
        private String dbConfigLogicDeleteField = "deleted";
        private Integer dbConfigLogicDeleteValue = 1;
        private Integer dbConfigLogicNotDeleteValue = 0;
    }
}
