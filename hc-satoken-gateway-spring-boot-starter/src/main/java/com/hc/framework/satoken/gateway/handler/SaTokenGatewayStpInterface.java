package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.stp.StpInterface;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 网关权限数据加载接口实现
 *
 * <p>实现 StpInterface 接口，为网关提供角色和权限数据加载能力。</p>
 * <p>通过 SaGatewayPermissionProvider 从远程服务获取权限数据。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenGatewayStpInterface implements StpInterface {

    private final SaGatewayPermissionProvider permissionProvider;

    /**
     * 构造器注入
     *
     * @param permissionProvider 权限数据提供者
     */
    public SaTokenGatewayStpInterface(SaGatewayPermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (permissionProvider == null) {
            log.debug("未配置 SaGatewayPermissionProvider，返回空权限列表");
            return new ArrayList<>();
        }
        try {
            List<String> permissions = permissionProvider.getPermissions(loginId);
            return permissions != null ? permissions : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取用户权限失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (permissionProvider == null) {
            log.debug("未配置 SaGatewayPermissionProvider，返回空角色列表");
            return new ArrayList<>();
        }
        try {
            List<String> roles = permissionProvider.getRoles(loginId);
            return roles != null ? roles : new ArrayList<>();
        } catch (Exception e) {
            log.error("获取用户角色失败: loginId={}, error={}", loginId, e.getMessage());
            return new ArrayList<>();
        }
    }
}
