package com.hc.framework.common.spi;

import java.util.List;

/**
 * 当前请求用户的角色/权限提供者 SPI
 *
 * <p>在微服务架构中，网关会将用户上下文（包含角色/权限）透传给下游服务。
 * 下游服务实现此接口，从请求上下文中直接获取角色/权限，
 * 避免跨服务 Redis 缓存不一致问题。</p>
 *
 * <p>优先级：UserContextPermissionProvider &gt; Redis缓存 &gt; SaPermissionProvider</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class UserContextPermissionProviderImpl implements UserContextPermissionProvider {
 *     @Override
 *     public List<String> getCurrentUserRoles() {
 *         UserContext ctx = UserContextHolder.get();
 *         return ctx != null ? ctx.getRoles() : null;
 *     }
 *
 *     @Override
 *     public List<String> getCurrentUserPermissions() {
 *         UserContext ctx = UserContextHolder.get();
 *         return ctx != null ? ctx.getPermissions() : null;
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface UserContextPermissionProvider {

    /**
     * 获取当前请求用户的角色列表
     *
     * @return 角色列表，如果当前无请求上下文则返回 null（回退到缓存/数据库查询）
     */
    List<String> getCurrentUserRoles();

    /**
     * 获取当前请求用户的权限列表
     *
     * @return 权限列表，如果当前无请求上下文则返回 null（回退到缓存/数据库查询）
     */
    List<String> getCurrentUserPermissions();
}
