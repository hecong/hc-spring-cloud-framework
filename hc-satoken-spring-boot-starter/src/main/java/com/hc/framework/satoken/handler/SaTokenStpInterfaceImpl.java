package com.hc.framework.satoken.handler;

import cn.dev33.satoken.stp.StpInterface;
import com.hc.framework.satoken.service.SaPermissionCacheService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据加载接口（支持缓存）
 *
 * <p>实现 StpInterface 接口，提供角色和权限数据加载能力，支持 Redis 缓存。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持 Redis 缓存角色和权限数据</li>
 *   <li>缓存未命中时自动从数据源加载</li>
 *   <li>支持自定义缓存过期时间</li>
 *   <li>支持缓存降级（Redis 不可用时直接查询）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class MyPermissionProvider implements SaPermissionProvider {
 *     @Override
 *     public List<String> getRoles(Long userId) {
 *         return userRoleMapper.selectRolesByUserId(userId);
 *     }
 *     @Override
 *     public List<String> getPermissions(Long userId) {
 *         return permissionMapper.selectPermsByUserId(userId);
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenStpInterfaceImpl implements StpInterface {

    private final SaPermissionProvider permissionProvider;
    private final SaPermissionCacheService cacheService;

    /**
     * 构造器注入（支持缓存）
     *
     * @param permissionProvider 权限数据提供者
     * @param cacheService       缓存服务
     */
    public SaTokenStpInterfaceImpl(SaPermissionProvider permissionProvider, SaPermissionCacheService cacheService) {
        this.permissionProvider = permissionProvider;
        this.cacheService = cacheService;
    }

    /**
     * 构造器注入（不支持缓存，兼容旧版本）
     *
     * @param permissionProvider 权限数据提供者
     */
    public SaTokenStpInterfaceImpl(SaPermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
        this.cacheService = null;
    }

    /**
     * 获取用户权限列表
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型（多账号体系时使用）
     * @return 权限列表
     */
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

            // 优先使用缓存
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

            // 无缓存服务，直接查询
            return permissionProvider.getPermissions(userId);
        } catch (Exception e) {
            log.error("获取用户权限失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户角色列表
     *
     * @param loginId   登录用户ID
     * @param loginType 登录类型
     * @return 角色列表
     */
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

            // 优先使用缓存
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

            // 无缓存服务，直接查询
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
