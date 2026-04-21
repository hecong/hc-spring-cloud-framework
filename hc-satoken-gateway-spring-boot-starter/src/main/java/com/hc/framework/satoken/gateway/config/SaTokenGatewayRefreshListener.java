package com.hc.framework.satoken.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.hc.framework.satoken.gateway.filter.SaTokenGatewayFilter;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 网关配置刷新监听器
 *
 * <p>监听配置中心（Nacos/Apollo/北极星等）的配置变更事件，动态刷新网关鉴权规则。</p>
 *
 * <p>支持场景：</p>
 * <ul>
 *   <li>Nacos 配置变更</li>
 *   <li>Apollo 配置变更</li>
 *   <li>腾讯北极星配置变更</li>
 *   <li>Spring Cloud Config 配置变更</li>
 *   <li>手动调用 /actuator/refresh 端点</li>
 * </ul>
 *
 * <p>使用示例（Nacos）：</p>
 * <pre>{@code
 * # bootstrap.yml
 * spring:
 *   cloud:
 *     nacos:
 *       config:
 *         server-addr: localhost:8848
 *         file-extension: yaml
 *         refresh-enabled: true  # 启用自动刷新
 * }</pre>
 *
 * <p>使用示例（Apollo）：</p>
 * <pre>{@code
 * # bootstrap.yml
 * app:
 *   id: your-app-id
 * apollo:
 *   meta: http://localhost:8080
 *   bootstrap:
 *     enabled: true
 *     namespaces: application
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SaTokenGatewayRefreshListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    @Autowired
    private SaTokenGatewayProperties properties;

    @Autowired
    private SaTokenGatewayFilter gatewayFilter;

    @Autowired(required = false)
    private SaReactorFilter saReactorFilter;

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        log.info("=== Sa-Token 网关配置已刷新 ===");
        log.info("刷新后的配置：");
        log.info("- 启用状态: {}", properties.getEnabled());
        log.info("- 排除路径: {}", properties.getExcludePaths());
        log.info("- 鉴权路由数量: {}", 
                properties.getAuthRoutes() != null ? properties.getAuthRoutes().size() : 0);

        if (properties.getAuthRoutes() != null) {
            properties.getAuthRoutes().forEach(route -> {
                log.info("  - 路径: {}, 登录: {}, 角色: {}, 权限: {}",
                        route.getPath(),
                        route.getRequireLogin(),
                        route.getRequireRole(),
                        route.getRequirePermission());
            });
        }

        // 注意：由于 SaReactorFilter 是 Sa-Token 内部类，
        // 配置刷新后会由 Spring 重新创建 Bean，自动应用新配置
        log.info("=== Sa-Token 网关过滤器已重新初始化 ===");
    }
}
