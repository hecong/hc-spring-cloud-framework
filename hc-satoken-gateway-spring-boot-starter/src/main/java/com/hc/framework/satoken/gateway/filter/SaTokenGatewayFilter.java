package com.hc.framework.satoken.gateway.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.common.model.DynamicAuthRoute;
import com.hc.framework.satoken.gateway.handler.SaGatewayDynamicRouteProvider;
import com.hc.framework.satoken.gateway.handler.SaTokenGatewayErrorBuilder;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties.AuthRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class SaTokenGatewayFilter {

    private final SaTokenGatewayProperties properties;
    private final SaGatewayDynamicRouteProvider dynamicRouteProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 构造器
     *
     * @param properties            网关配置属性
     * @param dynamicRouteProvider  动态路由提供者（可选）
     */
    public SaTokenGatewayFilter(SaTokenGatewayProperties properties,
                                SaGatewayDynamicRouteProvider dynamicRouteProvider) {
        this.properties = properties;
        this.dynamicRouteProvider = dynamicRouteProvider;
    }

    // 创建过滤器
    public SaReactorFilter createFilter() {
        String[] excludePaths = properties.getExcludePaths().toArray(new String[0]);

        return new SaReactorFilter()
            .addInclude("/**")
            .addExclude(excludePaths)
            // 核心：同步鉴权（权限数据已在 StpInterface 中响应式获取并缓存）
            .setAuth(obj -> {
                // 1. 登录校验
                StpUtil.checkLogin();
                
                // 2. 获取路径
                String path = SaHolder.getRequest().getRequestPath();
                
                // 3. 执行鉴权（Sa-Token 会通过 StpInterface 获取权限数据）
                doAuth(path);
            })
            .setError(e -> {
                log.warn("网关鉴权异常: {}", e.getMessage());
                return SaTokenGatewayErrorBuilder.buildErrorJson(e);
            });
    }

    // ===================== 鉴权逻辑 =====================
    private void doAuth(String path) {
        // 动态路由优先
        if (dynamicRouteProvider != null) {
            DynamicAuthRoute route = dynamicRouteProvider.matchRoute(path);
            if (route != null) {
                checkDynamicRoute(route);
                return;
            }
        }

        // 配置路由兜底
        checkConfigRoutes(path);
    }

    private void checkDynamicRoute(DynamicAuthRoute route) {
        // 角色校验
        if (route.hasRoleRequirement()) {
            boolean hasRole = route.getRequireRoles().stream().anyMatch(StpUtil::hasRole);
            if (!hasRole) {
                throw new NotRoleException(String.join(",", route.getRequireRoles()), StpUtil.getLoginType());
            }
        }

        // 权限校验
        if (route.hasPermissionRequirement()) {
            boolean hasPermission = route.getRequirePermissions().stream().anyMatch(StpUtil::hasPermission);
            if (!hasPermission) {
                throw new NotPermissionException(String.join(",", route.getRequirePermissions()), StpUtil.getLoginType());
            }
        }
    }

    private void checkConfigRoutes(String path) {
        if (CollectionUtils.isEmpty(properties.getAuthRoutes())) return;

        for (AuthRoute route : properties.getAuthRoutes()) {
            if (!pathMatcher.match(route.getPath(), path)) continue;

            // 角色校验
            if (hasText(route.getRequireRole())) {
                List<String> requireRoles = parseList(route.getRequireRole());
                boolean hasRole = requireRoles.stream().anyMatch(StpUtil::hasRole);
                if (!hasRole) {
                    throw new NotRoleException(route.getRequireRole(), StpUtil.getLoginType());
                }
            }

            // 权限校验
            if (hasText(route.getRequirePermission())) {
                List<String> requirePerms = parseList(route.getRequirePermission());
                boolean hasPermission = requirePerms.stream().anyMatch(StpUtil::hasPermission);
                if (!hasPermission) {
                    throw new NotPermissionException(route.getRequirePermission(), StpUtil.getLoginType());
                }
            }
        }
    }

    private boolean hasText(String s) { return s != null && !s.isBlank(); }
    private List<String> parseList(String s) {
        return Arrays.stream(s.split(","))
            .map(String::trim)
            .filter(i -> !i.isEmpty()).toList();
    }
}