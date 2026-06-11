package com.hc.framework.mybatis.spi;

import com.hc.framework.mybatis.model.DataScopeInfo;

/**
 * 数据范围提供者 SPI
 *
 * <p>业务模块实现此接口，根据用户ID和菜单权限编码返回最终有效数据范围。
 * 实现需处理多角色合并、部门递归展开、缓存等逻辑。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Service
 * public class DefaultDataScopeProviderImpl implements DataScopeProvider {
 *     private final DataScopeService dataScopeService;
 *
 *     @Override
 *     public DataScopeInfo getDataScope(Long userId, String permissionCode) {
 *         return dataScopeService.getOrComputeDataScope(userId, permissionCode);
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface DataScopeProvider {

    /**
     * 获取用户针对指定菜单的最终数据范围
     *
     * @param userId         用户ID
     * @param permissionCode 菜单权限编码（来自 Controller 的 {@code @SaCheckPermission} 注解）
     * @return 有效数据范围，不可返回 null
     */
    DataScopeInfo getDataScope(Long userId, String permissionCode);
}
