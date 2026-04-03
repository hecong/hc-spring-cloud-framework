package com.hc.framework.satoken.gateway.handler;

import java.util.List;

/**
 * 网关权限数据提供者接口
 *
 * <p>业务网关项目实现此接口，提供角色和权限数据。</p>
 * <p>与普通的 SaPermissionProvider 不同，此接口专为网关设计，支持从远程服务获取权限数据。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Component
 * public class GatewayPermissionProviderImpl implements SaGatewayPermissionProvider {
 *
 *     @Autowired
 *     private UserServiceClient userServiceClient;
 *
 *     @Override
 *     public List<String> getRoles(Object loginId) {
 *         // 从远程服务获取角色
 *         ResponseResult<List<String>> result = userServiceClient.getRoles(loginId);
 *         return result.getData();
 *     }
 *
 *     @Override
 *     public List<String> getPermissions(Object loginId) {
 *         // 从远程服务获取权限
 *         ResponseResult<List<String>> result = userServiceClient.getPermissions(loginId);
 *         return result.getData();
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface SaGatewayPermissionProvider {

    /**
     * 根据登录ID获取角色列表
     *
     * @param loginId 登录ID
     * @return 角色标识列表，如 ["admin", "user"]
     */
    List<String> getRoles(Object loginId);

    /**
     * 根据登录ID获取权限列表
     *
     * @param loginId 登录ID
     * @return 权限标识列表，如 ["user:add", "user:delete"]
     */
    List<String> getPermissions(Object loginId);
}
