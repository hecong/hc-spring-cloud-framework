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
     * 数据库类型（默认 MYSQL，可选：POSTGRE_SQL、ORACLE、SQL_SERVER 等）
     */
    private String dbType = "MYSQL";

    /**
     * 是否启用乐观锁拦截器
     */
    private boolean optimisticLockerEnabled = true;

    /**
     * 是否启用防全表更新/删除拦截器
     */
    private boolean blockAttackEnabled = true;

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
     * 数据权限配置
     */
    private DataPermissionConfig dataPermission = new DataPermissionConfig();

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

    /**
     * 数据权限配置
     */
    @Data
    public static class DataPermissionConfig {
        /**
         * 是否启用数据权限拦截器
         */
        private boolean enabled = true;

        /**
         * 部门ID展开上限（超出告警并截断）
         */
        private int maxDeptExpandSize = 500;

        /**
         * 降级策略：permit（放行）/ deny，默认（拒绝，返回 1=0）
         */
        private FailStrategy failStrategy = FailStrategy.DENY;

        /**
         * 额外的跳过表名（框架默认白名单 + 业务自定义白名单）
         */
        private java.util.Set<String> skipTables = new java.util.LinkedHashSet<>();
    }

    /**
     * 数据权限降级策略
     */
    public enum FailStrategy {
        /** 异常时放行（不追加条件，保证业务可用） */
        PERMIT,
        /** 异常时拒绝（追加 1=0，安全优先） */
        DENY
    }
}
