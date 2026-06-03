package com.hc.framework.satoken.handler;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.common.spi.UserContextPermissionProvider;
import com.hc.framework.satoken.service.SaPermissionCacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据加载接口（支持缓存 + 请求上下文优先）
 *
 * <p>权限数据获取优先级：</p>
 * <ol>
 *   <li>请求上下文（UserContextPermissionProvider） — 微服务场景下从网关透传的上下文获取，数据最新</li>
 *   <li>Redis 缓存（SaPermissionCacheService） — 减少数据库查询压力</li>
 *   <li>数据源（SaPermissionProvider） — 缓存未命中时回源查询</li>
 * </ol>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final SaPermissionProvider permissionProvider;
    private final SaPermissionCacheService cacheService;
    private final UserContextPermissionProvider userContextPermissionProvider;

    /**
     * 构造器注入（支持缓存 + 请求上下文）
     */
    public SaTokenStpInterfaceImpl(SaPermissionProvider permissionProvider,
                                   SaPermissionCacheService cacheService,
                                   UserContextPermissionProvider userContextPermissionProvider) {
        this.permissionProvider = permissionProvider;
        this.cacheService = cacheService;
        this.userContextPermissionProvider = userContextPermissionProvider;
    }

    /**
     * 构造器注入（支持缓存，无请求上下文）
     */
    public SaTokenStpInterfaceImpl(SaPermissionProvider permissionProvider,
                                   SaPermissionCacheService cacheService) {
        this(permissionProvider, cacheService, null);
    }

    /**
     * 构造器注入（不支持缓存，兼容旧版本）
     */
    public SaTokenStpInterfaceImpl(SaPermissionProvider permissionProvider) {
        this(permissionProvider, null, null);
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (permissionProvider == null) {
            log.debug("未配置 SaPermissionProvider，返回空权限列表");
            return new ArrayList<>();
        }

        try {
            Long userId = convertToLong(loginId);
            if (userId == null) {
                return new ArrayList<>();
            }

            // 1. 优先从请求上下文获取（微服务场景下由网关透传，数据最新）
            if (userContextPermissionProvider != null) {
                List<String> permissions = userContextPermissionProvider.getCurrentUserPermissions();
                if (permissions != null) {
                    log.debug("从请求上下文获取用户权限: userId={}, count={}", userId, permissions.size());
                    return permissions;
                }
            }

            // 2. 从 Redis 缓存获取
            if (cacheService != null) {
                return cacheService.getPermissions(userId, () -> {
                    try {
                        return permissionProvider.getPermissions(userId);
                    } catch (Exception e) {
                        log.error("获取用户权限失败: userId={}, error={}", userId, e.getMessage());
                        return new ArrayList<>();
                    }
                });
            }

            // 3. 直接查询数据源
            return permissionProvider.getPermissions(userId);
        } catch (Exception e) {
            log.error("获取用户权限失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (permissionProvider == null) {
            log.debug("未配置 SaPermissionProvider，返回空角色列表");
            return new ArrayList<>();
        }

        try {
            Long userId = convertToLong(loginId);
            if (userId == null) {
                return new ArrayList<>();
            }

            // 1. 优先从请求上下文获取
            if (userContextPermissionProvider != null) {
                List<String> roles = userContextPermissionProvider.getCurrentUserRoles();
                if (roles != null) {
                    log.debug("从请求上下文获取用户角色: userId={}, count={}", userId, roles.size());
                    return roles;
                }
            }

            // 2. 从 Redis 缓存获取
            if (cacheService != null) {
                return cacheService.getRoles(userId, () -> {
                    try {
                        return permissionProvider.getRoles(userId);
                    } catch (Exception e) {
                        log.error("获取用户角色失败: userId={}, error={}", userId, e.getMessage());
                        return new ArrayList<>();
                    }
                });
            }

            // 3. 直接查询数据源
            return permissionProvider.getRoles(userId);
        } catch (Exception e) {
            log.error("获取用户角色失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将 loginId 转换为 Long 类型
     */
    private Long convertToLong(Object loginId) {
        if (loginId == null) {
            return null;
        }
        if (loginId instanceof Long) {
            return (Long) loginId;
        }
        if (loginId instanceof Number) {
            return ((Number) loginId).longValue();
        }
        try {
            return Long.parseLong(loginId.toString());
        } catch (NumberFormatException e) {
            log.warn("无法将 loginId 转换为 Long: {}", loginId);
            return null;
        }
    }
}
