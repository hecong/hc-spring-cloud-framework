package com.hc.framework.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.AntPathMatcher;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 动态路由权限规则
 *
 * <p>表示一条 URL-权限映射规则，用于网关动态鉴权。</p>
 *
 * <p>与配置文件 AuthRoute 的区别：</p>
 * <ul>
 *   <li>支持从数据库/远程服务加载</li>
 *   <li>支持动态刷新</li>
 *   <li>支持更丰富的元数据（如服务名、描述等）</li>
 * </ul>
 *
 * <p>数据来源建议：</p>
 * <ul>
 *   <li>从菜单表获取 URL 路径和所需权限</li>
 *   <li>从角色权限关联表获取所需角色</li>
 *   <li>通过 RPC 或 HTTP 接口同步到网关</li>
 * </ul>
 *
 * <p>数据库设计示例：</p>
 * <pre>{@code
 * -- 菜单表
 * CREATE TABLE sys_menu (
 *     id BIGINT PRIMARY KEY,
 *     name VARCHAR(50),
 *     path VARCHAR(200),           -- 前端路由路径
 *     api_path VARCHAR(200),       -- API 路径，如 /api/user/**
 *     permission VARCHAR(100),     -- 权限标识，如 user:list
 *     require_login TINYINT,       -- 是否需要登录
 *     ...
 * );
 *
 * -- 角色菜单关联表
 * CREATE TABLE sys_role_menu (
 *     role_id BIGINT,
 *     menu_id BIGINT,
 *     PRIMARY KEY (role_id, menu_id)
 * );
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicAuthRoute implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 路径模式（Ant 风格，如 /api/user/**）
     */
    private String path;

    /**
     * 是否需要登录
     */
    @Builder.Default
    private Boolean requireLogin = true;

    /**
     * 需要的角色列表
     */
    private List<String> requireRoles;

    /**
     * 需要的权限列表
     */
    private List<String> requirePermissions;

    /**
     * 多角色/权限的匹配模式
     * <p>ANY：拥有任一角色/权限即可（默认）</p>
     * <p>ALL：必须拥有全部角色/权限</p>
     */
    @Builder.Default
    private MatchMode matchMode = MatchMode.ANY;

    /**
     * 关联的服务名（可选，用于调试和日志）
     */
    private String serviceName;

    /**
     * 规则描述（可选，用于调试和日志）
     */
    private String description;

    /**
     * 优先级（可选，数值越小优先级越高）
     */
    @Builder.Default
    private Integer priority = 100;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 判断请求路径是否匹配此规则
     *
     * @param requestPath 请求路径
     * @return 是否匹配
     */
    public boolean matches(String requestPath) {
        if (path == null || requestPath == null) {
            return false;
        }
        return PATH_MATCHER.match(path, requestPath);
    }

    /**
     * 是否需要角色校验
     */
    public boolean hasRoleRequirement() {
        return requireRoles != null && !requireRoles.isEmpty();
    }

    /**
     * 是否需要权限校验
     */
    public boolean hasPermissionRequirement() {
        return requirePermissions != null && !requirePermissions.isEmpty();
    }

    /**
     * 获取角色列表（逗号分隔）
     */
    public String getRequireRole() {
        if (requireRoles == null || requireRoles.isEmpty()) {
            return null;
        }
        return String.join(",", requireRoles);
    }

    /**
     * 获取权限列表（逗号分隔）
     */
    public String getRequirePermission() {
        if (requirePermissions == null || requirePermissions.isEmpty()) {
            return null;
        }
        return String.join(",", requirePermissions);
    }

    /**
     * 角色/权限匹配模式
     */
    public enum MatchMode {
        /** 拥有任一即可 */
        ANY,
        /** 必须全部拥有 */
        ALL
    }
}
