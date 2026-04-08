package com.hc.framework.satoken.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties.AuthRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sa-Token 网关鉴权过滤器
 *
 * <p>基于 Spring Cloud Gateway 和 Sa-Token Reactor 实现统一鉴权。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持 Ant 风格路径匹配</li>
 *   <li>支持登录校验</li>
 *   <li>支持角色校验</li>
 *   <li>支持权限校验</li>
 *   <li>支持自定义排除路径</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenGatewayFilter {

    private final SaTokenGatewayProperties properties;

    /**
     * 构造器注入
     *
     * @param properties 网关配置属性
     */
    public SaTokenGatewayFilter(SaTokenGatewayProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 Sa-Token Reactor 过滤器
     *
     * @return SaReactorFilter 实例
     */
    public SaReactorFilter createFilter() {
        // 构建排除路径数组
        String[] excludePaths = properties.getExcludePaths().toArray(new String[0]);

        return new SaReactorFilter()
                // 拦截所有路径
                .addInclude("/**")
                // 排除路径
                .addExclude(excludePaths)
                // 鉴权规则
                .setAuth(obj -> {
                    // 遍历所有鉴权路由配置
                    if (!CollectionUtils.isEmpty(properties.getAuthRoutes())) {
                        for (AuthRoute route : properties.getAuthRoutes()) {
                            // 登录校验
                            if (Boolean.TRUE.equals(route.getRequireLogin())) {
                                SaRouter.match(route.getPath(), StpUtil::checkLogin);
                            }
                            // 角色校验
                            if (hasText(route.getRequireRole())) {
                                List<String> roles = parseList(route.getRequireRole());
                                SaRouter.match(route.getPath(), () -> {
                                    boolean hasAnyRole = roles.stream().anyMatch(StpUtil::hasRole);
                                    if (!hasAnyRole) {
                                        throw new cn.dev33.satoken.exception.NotRoleException(
                                                String.join(",", roles), StpUtil.getLoginType());
                                    }
                                });
                            }
                            // 权限校验
                            if (hasText(route.getRequirePermission())) {
                                List<String> permissions = parseList(route.getRequirePermission());
                                SaRouter.match(route.getPath(), () -> {
                                    boolean hasAnyPermission = permissions.stream()
                                            .anyMatch(StpUtil::hasPermission);
                                    if (!hasAnyPermission) {
                                        throw new cn.dev33.satoken.exception.NotPermissionException(
                                                String.join(",", permissions), StpUtil.getLoginType());
                                    }
                                });
                            }
                        }
                    }
                })
                // 异常处理 - 直接抛出异常，由 SaTokenGatewayExceptionHandler 统一处理
                // 这样可以确保返回统一的 Result 格式，支持自定义字段名
                .setError(e -> {
                    throw new RuntimeException(e);
                });
    }

    /**
     * 判断字符串是否有内容
     *
     * @param str 字符串
     * @return true=有内容
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 解析逗号分隔的字符串为列表
     *
     * @param str 字符串
     * @return 列表
     */
    private List<String> parseList(String str) {
        if (str == null || str.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
