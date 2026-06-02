package com.hc.framework.satoken.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.hc.framework.satoken.gateway.filter.SaTokenGatewayFilter;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayErrorBuilder;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayExceptionHandler;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

/**
 * Sa-Token 网关自动配置类
 *
 * <p>网关只做登录校验（Token 认证），不做角色/权限校验。
 * 权限控制全部下沉到微服务层。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SaTokenGatewayProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "hc.satoken.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenGatewayAutoConfiguration {

    /**
     * 网关错误响应构建器
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenGatewayErrorBuilder saTokenGatewayErrorBuilder(SaTokenGatewayProperties properties) {
        return new SaTokenGatewayErrorBuilder(properties);
    }

    /**
     * Sa-Token 网关过滤器
     * <p>只做登录校验，不做角色/权限校验。</p>
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public SaTokenGatewayFilter saTokenGatewayFilter(
            SaTokenGatewayProperties properties,
            SaTokenGatewayErrorBuilder errorBuilder) {
        return new SaTokenGatewayFilter(properties, errorBuilder);
    }

    /**
     * Sa-Token Reactor 过滤器
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public SaReactorFilter saReactorFilter(SaTokenGatewayFilter gatewayFilter) {
        return gatewayFilter.createFilter();
    }

    /**
     * Sa-Token 网关全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenGatewayExceptionHandler saTokenGatewayExceptionHandler(
            SaTokenGatewayErrorBuilder errorBuilder) {
        return new SaTokenGatewayExceptionHandler(errorBuilder);
    }

    /**
     * 配置刷新监听器
     */
    @Bean
    public SaTokenGatewayRefreshListener saTokenGatewayRefreshListener() {
        return new SaTokenGatewayRefreshListener();
    }
}
