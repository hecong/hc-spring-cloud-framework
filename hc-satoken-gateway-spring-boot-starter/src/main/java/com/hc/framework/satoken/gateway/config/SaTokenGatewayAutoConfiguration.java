package com.hc.framework.satoken.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpInterface;
import com.hc.framework.satoken.gateway.filter.SaTokenGatewayFilter;
import com.hc.framework.satoken.gateway.handler.SaGatewayPermissionProvider;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayExceptionHandler;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayStpInterface;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>业务项目需要实现 {@link SaGatewayPermissionProvider} 接口提供权限数据：</p>
 * <pre>{@code
 * @Component
 * public class GatewayPermissionProviderImpl implements SaGatewayPermissionProvider {
 *     @Override
 *     public List<String> getRoles(Object loginId) {
 *         // 从远程服务获取角色
 *         return userServiceClient.getRoles(loginId).getData();
 *     }
 *
 *     @Override
 *     public List<String> getPermissions(Object loginId) {
 *         // 从远程服务获取权限
 *         return userServiceClient.getPermissions(loginId).getData();
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SaTokenGatewayProperties.class)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
@ConditionalOnProperty(prefix = "hc.satoken.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenGatewayAutoConfiguration {

    private final SaTokenGatewayProperties properties;

    /**
     * 构造器注入
     *
     * @param properties 网关配置属性
     */
    public SaTokenGatewayAutoConfiguration(SaTokenGatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * Sa-Token 网关过滤器
     *
     * <p>核心过滤器，处理网关层的统一鉴权。</p>
     * <p>添加 @RefreshScope 支持配置中心动态刷新。</p>
     */
    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    public SaTokenGatewayFilter saTokenGatewayFilter() {
        log.info("=== Sa-Token 网关鉴权过滤器已初始化 ===");
        return new SaTokenGatewayFilter(properties);
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

    /**
     * 默认权限提供者（空实现）
     *
     * <p>当业务项目没有提供 SaGatewayPermissionProvider 实现时，使用此默认实现。</p>
     * <p>返回空的角色和权限列表，适用于仅需要登录认证的场景。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SaGatewayPermissionProvider.class)
    public SaGatewayPermissionProvider defaultSaGatewayPermissionProvider() {
        log.warn("未配置 SaGatewayPermissionProvider，使用默认空实现（仅支持登录认证）");
        return new SaGatewayPermissionProvider() {
            @Override
            public java.util.List<String> getRoles(Object loginId) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> getPermissions(Object loginId) {
                return java.util.Collections.emptyList();
            }
        };
    }

    /**
     * Sa-Token 权限数据加载接口实现
     *
     * <p>实现 StpInterface 接口，为网关提供角色和权限数据加载能力。</p>
     */
    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface(SaGatewayPermissionProvider permissionProvider) {
        return new SaTokenGatewayStpInterface(permissionProvider);
    }
}
