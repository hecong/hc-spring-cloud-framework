package com.hc.framework.satoken.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.hc.framework.satoken.gateway.filter.SaTokenGatewayFilter;
import com.hc.framework.satoken.gateway.handler.SaGatewayDynamicRouteProvider;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayExceptionHandler;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

/**
 * Sa-Token 网关鉴权自动配置类
 *
 * <p>为 Spring Cloud Gateway 提供统一鉴权能力。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>hc.satoken.gateway.enabled: 是否启用网关鉴权（默认 true）</li>
 *   <li>hc.satoken.gateway.auth-routes: 需要鉴权的路由列表</li>
 *   <li>hc.satoken.gateway.exclude-paths: 排除路径列表</li>
 *   <li>hc.satoken.gateway.forward-token: 是否转发 Token 给下游服务</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     gateway:
 *       enabled: true
 *       auth-routes:
 *         - path: /api/**
 *           require-login: true
 *         - path: /admin/**
 *           require-login: true
 *           require-role: admin
 *       exclude-paths:
 *         - /api/auth/login
 *         - /api/public/**
 *       forward-token: true
 * }</pre>
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
     * Sa-Token 网关过滤器
     *
     * <p>核心过滤器，处理网关层的统一鉴权。</p>
     * <p>添加 @RefreshScope 支持配置中心动态刷新。</p>
     * <p>支持动态路由权限规则（优先级高于配置文件）。</p>
     *
     * @param dynamicRouteProvider 动态路由提供者（可选，业务实现）
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public SaTokenGatewayFilter saTokenGatewayFilter(
        SaTokenGatewayProperties properties,
        @Autowired(required = false) SaGatewayDynamicRouteProvider dynamicRouteProvider) {
        if (dynamicRouteProvider != null) {
            log.info("=== 动态路由权限提供者已启用，优先级高于配置文件 ===");
        }
        return new SaTokenGatewayFilter(properties, dynamicRouteProvider);
    }

    /**
     * Sa-Token Reactor 过滤器
     *
     * <p>注册到 Spring Cloud Gateway 过滤器链。</p>
     * <p>添加 @RefreshScope 支持配置中心动态刷新。</p>
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public SaReactorFilter saReactorFilter(SaTokenGatewayFilter gatewayFilter) {
        return gatewayFilter.createFilter();
    }

    /**
     * Sa-Token 网关全局异常处理器
     *
     * <p>统一处理 Sa-Token 相关异常，转换为框架统一的 Result 响应格式。</p>
     * <p>支持自定义字段名（通过 hc.web.code-field 等配置）。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenGatewayExceptionHandler saTokenGatewayExceptionHandler() {
        return new SaTokenGatewayExceptionHandler();
    }

    /**
     * 配置刷新监听器
     *
     * <p>监听配置中心变更事件，输出刷新日志。</p>
     */
    @Bean
    public SaTokenGatewayRefreshListener saTokenGatewayRefreshListener() {
        return new SaTokenGatewayRefreshListener();
    }


}
