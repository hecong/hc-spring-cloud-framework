package com.hc.framework.satoken.gateway.config;

import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 网关配置刷新监听器
 *
 * <p>监听配置中心（Nacos/Apollo/北极星等）的配置变更事件，输出刷新日志。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SaTokenGatewayRefreshListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    @Autowired
    private SaTokenGatewayProperties properties;

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        log.info("=== Sa-Token 网关配置已刷新 ===");
        log.info("- 启用状态: {}", properties.getEnabled());
        log.info("- 排除路径: {}", properties.getExcludePaths());
        log.info("- 转发Token: {}", properties.getForwardToken());
        log.info("=== Sa-Token 网关过滤器已重新初始化 ===");
    }
}
