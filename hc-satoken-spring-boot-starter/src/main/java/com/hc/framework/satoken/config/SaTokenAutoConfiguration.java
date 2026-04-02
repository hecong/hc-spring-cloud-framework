package com.hc.framework.satoken.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Sa-Token Starter 自动配置类
 * 提供权限认证功能
 */
@AutoConfiguration
@EnableConfigurationProperties(SaTokenProperties.class)
@ConditionalOnProperty(prefix = "hc.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(SaTokenConfiguration.class)
public class SaTokenAutoConfiguration {

}
