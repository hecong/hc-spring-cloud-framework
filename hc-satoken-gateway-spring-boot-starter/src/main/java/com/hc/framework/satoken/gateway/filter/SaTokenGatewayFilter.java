package com.hc.framework.satoken.gateway.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayErrorBuilder;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Sa-Token 网关过滤器
 *
 * <p>职责：只做登录校验（Token 认证），不做角色/权限校验。
 * 权限控制全部下沉到微服务层通过 {@code @SaCheckPermission} / {@code @SaCheckRole} 实现。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenGatewayFilter {

    private final SaTokenGatewayProperties properties;
    private final SaTokenGatewayErrorBuilder errorBuilder;

    public SaTokenGatewayFilter(SaTokenGatewayProperties properties,
                                SaTokenGatewayErrorBuilder errorBuilder) {
        this.properties = properties;
        this.errorBuilder = errorBuilder;
    }

    /**
     * 创建 SaReactorFilter
     * <p>只做登录校验，不校验角色/权限。</p>
     */
    public SaReactorFilter createFilter() {
        String[] excludePaths = properties.getExcludePaths().toArray(new String[0]);

        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude(excludePaths)
                .setAuth(obj -> {
                    // 只做登录校验
                    StpUtil.checkLogin();
                })
                .setError(e -> {
                    log.warn("网关认证异常: {}", e.getMessage());
                    String path = SaHolder.getRequest().getRequestPath();
                    return errorBuilder.buildErrorJson(e, path);
                });
    }
}
