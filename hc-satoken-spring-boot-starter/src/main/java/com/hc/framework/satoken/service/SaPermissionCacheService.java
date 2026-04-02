package com.hc.framework.satoken.service;

import com.hc.framework.redis.util.RedisCacheUtils;
import com.hc.framework.satoken.config.SaTokenProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限缓存服务
 *
 * <p>使用 Redis 缓存用户的角色和权限信息，避免每次请求都查询数据库。</p>
 *
 * <p>缓存 Key 规则：</p>
 * <ul>
 *   <li>角色缓存 Key：satoken:cache:roles:{userId}</li>
 *   <li>权限缓存 Key：satoken:cache:permissions:{userId}</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private SaPermissionCacheService cacheService;
 *
 * // 获取角色（优先从缓存获取）
 * List<String> roles = cacheService.getRoles(userId, () -> permissionProvider.getRoles(userId));
 *
 * // 清除用户缓存
 * cacheService.clearUserCache(userId);
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaPermissionCacheService {

    /**
     * 缓存 Key 前缀
     */
    private static final String CACHE_PREFIX = "satoken:cache:";

    /**
     * 角色缓存后缀
     */
    private static final String ROLES_SUFFIX = ":roles:";

    /**
     * 权限缓存后缀
     */
    private static final String PERMISSIONS_SUFFIX = ":permissions:";

    private final RedisCacheUtils redisCacheUtils;
    private final SaTokenProperties properties;

    /**
     * 构造器注入
     */
    public SaPermissionCacheService(RedisCacheUtils redisCacheUtils, SaTokenProperties properties) {
        this.redisCacheUtils = redisCacheUtils;
        this.properties = properties;
    }

    /**
     * 获取用户角色列表（优先从缓存获取）
     *
     * @param userId 用户ID
     * @param loader 数据加载器（缓存未命中时调用）
     * @return 角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(Long userId, RoleLoader loader) {
        if (!isCacheEnabled()) {
            return loader.load();
        }

        String cacheKey = getRolesCacheKey(userId);
        try {
            List<String> roles = redisCacheUtils.get(cacheKey);
            if (roles != null) {
                log.debug("从缓存获取用户角色: userId={}, roles={}", userId, roles);
                return roles;
            }

            // 缓存未命中，加载数据
            roles = loader.load();
            if (roles == null) {
                roles = Collections.emptyList();
            }

            // 写入缓存
            long timeout = getCacheTimeout();
            redisCacheUtils.set(cacheKey, roles, timeout, TimeUnit.SECONDS);
            log.debug("缓存用户角色: userId={}, roles={}, timeout={}s", userId, roles, timeout);

            return roles;
        } catch (Exception e) {
            log.error("获取角色缓存失败，降级为直接查询: userId={}, error={}", userId, e.getMessage());
            return loader.load();
        }
    }

    /**
     * 获取用户权限列表（优先从缓存获取）
     *
     * @param userId 用户ID
     * @param loader 数据加载器（缓存未命中时调用）
     * @return 权限列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Long userId, PermissionLoader loader) {
        if (!isCacheEnabled()) {
            return loader.load();
        }

        String cacheKey = getPermissionsCacheKey(userId);
        try {
            List<String> permissions = redisCacheUtils.get(cacheKey);
            if (permissions != null) {
                log.debug("从缓存获取用户权限: userId={}, permissions={}", userId, permissions);
                return permissions;
            }

            // 缓存未命中，加载数据
            permissions = loader.load();
            if (permissions == null) {
                permissions = Collections.emptyList();
            }

            // 写入缓存
            long timeout = getCacheTimeout();
            redisCacheUtils.set(cacheKey, permissions, timeout, TimeUnit.SECONDS);
            log.debug("缓存用户权限: userId={}, permissions={}, timeout={}s", userId, permissions, timeout);

            return permissions;
        } catch (Exception e) {
            log.error("获取权限缓存失败，降级为直接查询: userId={}, error={}", userId, e.getMessage());
            return loader.load();
        }
    }

    /**
     * 清除用户缓存（角色 + 权限）
     *
     * @param userId 用户ID
     */
    public void clearUserCache(Long userId) {
        if (!isCacheEnabled()) {
            return;
        }

        try {
            redisCacheUtils.delete(getRolesCacheKey(userId));
            redisCacheUtils.delete(getPermissionsCacheKey(userId));
            log.info("清除用户权限缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 清除所有用户权限缓存
     */
    public void clearAllCache() {
        if (!isCacheEnabled()) {
            return;
        }

        try {
            redisCacheUtils.deleteByPrefix(CACHE_PREFIX);
            log.info("清除所有用户权限缓存");
        } catch (Exception e) {
            log.error("清除所有缓存失败: error={}", e.getMessage());
        }
    }

    /**
     * 刷新用户缓存（强制重新加载）
     *
     * @param userId 用户ID
     * @param roles       角色列表
     * @param permissions 权限列表
     */
    public void refreshUserCache(Long userId, List<String> roles, List<String> permissions) {
        if (!isCacheEnabled()) {
            return;
        }

        try {
            long timeout = getCacheTimeout();

            if (roles != null) {
                redisCacheUtils.set(getRolesCacheKey(userId), roles, timeout, TimeUnit.SECONDS);
            }

            if (permissions != null) {
                redisCacheUtils.set(getPermissionsCacheKey(userId), permissions, timeout, TimeUnit.SECONDS);
            }

            log.info("刷新用户权限缓存: userId={}, roles={}, permissions={}", userId, roles, permissions);
        } catch (Exception e) {
            log.error("刷新用户缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 判断缓存是否启用
     */
    private boolean isCacheEnabled() {
        return Boolean.TRUE.equals(properties.getPermission().getCacheEnabled());
    }

    /**
     * 获取缓存超时时间（秒）
     */
    private long getCacheTimeout() {
        Long timeout = properties.getPermission().getCacheTimeout();
        return timeout != null && timeout > 0 ? timeout : 300L;
    }

    /**
     * 获取角色缓存 Key
     */
    private String getRolesCacheKey(Long userId) {
        return CACHE_PREFIX + ROLES_SUFFIX + userId;
    }

    /**
     * 获取权限缓存 Key
     */
    private String getPermissionsCacheKey(Long userId) {
        return CACHE_PREFIX + PERMISSIONS_SUFFIX + userId;
    }

    // ==================== 函数式接口 ====================

    /**
     * 角色数据加载器
     */
    @FunctionalInterface
    public interface RoleLoader {
        List<String> load();
    }

    /**
     * 权限数据加载器
     */
    @FunctionalInterface
    public interface PermissionLoader {
        List<String> load();
    }
}
