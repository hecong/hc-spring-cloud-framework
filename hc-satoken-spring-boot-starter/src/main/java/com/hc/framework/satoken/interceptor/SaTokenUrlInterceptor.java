package com.hc.framework.satoken.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.config.SaTokenProperties;
import com.hc.framework.satoken.config.SaTokenProperties.UrlPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * Sa-Token URL 权限拦截器
 *
 * <p>基于配置文件的 URL 权限拦截，支持 Ant 风格路径匹配。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持 Ant 风格路径匹配（如 /admin/**）</li>
 *   <li>支持角色校验</li>
 *   <li>支持权限校验</li>
 *   <li>支持登录校验开关</li>
 * </ul>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     permission:
 *       enabled: true
 *       url-permissions:
 *         - path: /admin/**
 *           role: admin
 *         - path: /user/**
 *           permission: user:view
 *         - path: /public/**
 *           require-login: false
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenUrlInterceptor implements HandlerInterceptor {

    private final SaTokenProperties properties;
    private final PathMatcher pathMatcher;

    /**
     * 构造器注入
     */
    public SaTokenUrlInterceptor(SaTokenProperties properties) {
        this.properties = properties;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 权限校验未启用，放行
        if (!Boolean.TRUE.equals(properties.getPermission().getEnabled())) {
            return true;
        }

        String requestPath = request.getRequestURI();

        // 检查是否在排除路径中
        if (isExcludedPath(requestPath)) {
            log.debug("路径在排除列表中，放行: {}", requestPath);
            return true;
        }

        // 查找匹配的权限规则
        UrlPermission matchedPermission = findMatchedPermission(requestPath);
        if (matchedPermission == null) {
            // 没有匹配的规则，默认需要登录
            checkLogin(requestPath);
            return true;
        }

        // 执行权限校验
        checkPermission(requestPath, matchedPermission);

        return true;
    }

    /**
     * 检查路径是否在排除列表中
     */
    private boolean isExcludedPath(String requestPath) {
        List<String> excludePaths = properties.getAllExcludePaths();
        for (String excludePath : excludePaths) {
            if (pathMatcher.match(excludePath, requestPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找匹配的 URL 权限规则
     */
    private UrlPermission findMatchedPermission(String requestPath) {
        List<UrlPermission> urlPermissions = properties.getPermission().getUrlPermissions();
        for (UrlPermission permission : urlPermissions) {
            if (permission.getPath() != null && pathMatcher.match(permission.getPath(), requestPath)) {
                return permission;
            }
        }
        return null;
    }

    /**
     * 检查登录状态
     */
    private void checkLogin(String requestPath) {
        if (!StpUtil.isLogin()) {
            log.warn("访问需要登录的路径，但用户未登录: path={}", requestPath);
            StpUtil.checkLogin();
        }
    }

    /**
     * 执行权限校验
     */
    private void checkPermission(String requestPath, UrlPermission permission) {
        // 不需要登录
        if (Boolean.FALSE.equals(permission.getRequireLogin())) {
            log.debug("路径不需要登录: path={}", requestPath);
            return;
        }

        // 检查登录
        if (!StpUtil.isLogin()) {
            log.warn("访问需要登录的路径，但用户未登录: path={}", requestPath);
            StpUtil.checkLogin();
        }

        // 检查角色
        if (hasText(permission.getRole())) {
            List<String> requiredRoles = parseList(permission.getRole());
            boolean hasRole = matchPermission(requiredRoles, StpUtil::hasRole, permission.getMatchMode());
            if (!hasRole) {
                log.warn("用户缺少所需角色: path={}, required={}, current={}",
                        requestPath, requiredRoles, StpUtil.getRoleList());
                StpUtil.checkRole(permission.getRole());
            }
        }

        // 检查权限
        if (hasText(permission.getPermission())) {
            List<String> requiredPerms = parseList(permission.getPermission());
            boolean hasPerm = matchPermission(requiredPerms, StpUtil::hasPermission, permission.getMatchMode());
            if (!hasPerm) {
                log.warn("用户缺少所需权限: path={}, required={}, current={}",
                        requestPath, requiredPerms, StpUtil.getPermissionList());
                StpUtil.checkPermission(permission.getPermission());
            }
        }

        log.debug("权限校验通过: path={}, userId={}", requestPath, StpUtil.getLoginId());
    }

    /**
     * 解析逗号分隔的字符串为列表
     */
    private List<String> parseList(String str) {
        if (str == null || str.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 判断字符串是否有内容
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 根据匹配模式校验角色/权限
     *
     * @param requiredList 所需角色/权限列表
     * @param checker      校验函数（hasRole 或 hasPermission）
     * @param matchMode    匹配模式：ANY=任一即可，ALL=必须全部
     */
    private boolean matchPermission(List<String> requiredList, java.util.function.Predicate<String> checker,
                                    SaTokenProperties.MatchMode matchMode) {
        if (matchMode == SaTokenProperties.MatchMode.ALL) {
            return requiredList.stream().allMatch(checker);
        }
        return requiredList.stream().anyMatch(checker);
    }
}
