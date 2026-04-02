package com.hc.framework.mybatis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * MyBatis-Plus Starter 自动配置类
 * 提供MyBatis-Plus + 多数据源支持
 */
@AutoConfiguration
@EnableConfigurationProperties(MyBatisPlusProperties.class)
@ConditionalOnProperty(prefix = "hc.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(MybatisPlusConfig.class)
public class MyBatisPlusAutoConfiguration {

}
