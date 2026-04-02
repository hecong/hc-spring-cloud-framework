package com.hc.framework.satoken.handler;

import java.util.List;

/**
 * 权限数据提供者接口
 *
 * <p>业务项目实现此接口，提供角色和权限数据。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Service
 * public class MyPermissionProviderImpl implements SaPermissionProvider {
 *
 *     @Autowired
 *     private UserRoleService userRoleService;
 *
 *     @Override
 *     public List<String> getRoles(Long userId) {
 *         return userRoleService.getRolesByUserId(userId);
 *     }
 *
 *     @Override
 *     public List<String> getPermissions(Long userId) {
 *         return userRoleService.getPermissionsByUserId(userId);
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface SaPermissionProvider {

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色标识列表，如 ["admin", "user"]
     */
    List<String> getRoles(Long userId);

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限标识列表，如 ["user:add", "user:delete"]
     */
    List<String> getPermissions(Long userId);
}
