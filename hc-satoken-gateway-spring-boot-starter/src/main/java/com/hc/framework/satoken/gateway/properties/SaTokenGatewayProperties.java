package com.hc.framework.satoken.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 网关鉴权配置属性
 *
 * <p>支持配置中心动态刷新（Nacos/Apollo/北极星等）。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>hc.satoken.gateway.enabled: 是否启用网关鉴权（默认 true）</li>
 *   <li>hc.satoken.gateway.auth-routes: 需要鉴权的路由列表</li>
 *   <li>hc.satoken.gateway.exclude-paths: 排除路径列表</li>
 *   <li>hc.satoken.gateway.forward-token: 是否转发 Token 给下游服务</li>
 * </ul>
 *
 * <p>Nacos 配置示例：</p>
 * <pre>{@code
 * # bootstrap.yml
 * spring:
 *   application:
 *     name: gateway-service
 *   cloud:
 *     nacos:
 *       config:
 *         server-addr: localhost:8848
 *         file-extension: yaml
 *         refresh-enabled: true  # 关键：启用自动刷新
 *         namespace: your-namespace-id
 *         group: DEFAULT_GROUP
 * }</pre>
 *
 * <p>Apollo 配置示例：</p>
 * <pre>{@code
 * # bootstrap.yml
 * app:
 *   id: gateway-service
 * apollo:
 *   meta: http://localhost:8080
 *   bootstrap:
 *     enabled: true
 *     namespaces: application
 *     eagerLoad:
 *       enabled: true  # 提前加载配置
 * }</pre>
 *
 * <p>腾讯北极星配置示例：</p>
 * <pre>{@code
 * # bootstrap.yml
 * spring:
 *   cloud:
 *     polaris:
 *       address: grpc://localhost:8091
 *       config:
 *         auto-refresh: true  # 启用自动刷新
 * }</pre>
 *
 * <p>配置内容示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     gateway:
 *       enabled: true
 *       auth-routes:
 *         - path: /api/**
 *           require-login: true
 *         - path: /admin/**
 *           require-login: true
 *           require-role: admin
 *         - path: /user/**
 *           require-login: true
 *           require-permission: user:read
 *       exclude-paths:
 *         - /api/auth/login
 *         - /api/auth/register
 *         - /api/public/**
 *       forward-token: true
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "hc.satoken.gateway")
public class SaTokenGatewayProperties {

    /**
     * 是否启用网关鉴权
     */
    private Boolean enabled = true;

    /**
     * 需要鉴权的路由列表
     */
    private List<AuthRoute> authRoutes = new ArrayList<>();

    /**
     * 排除路径列表（支持 Ant 风格通配符）
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 是否将 Token 转发给下游服务
     */
    private Boolean forwardToken = true;

    /**
     * Token 转发时使用的 Header 名称
     */
    private String forwardHeaderName = "Authorization";

    /**
     * 鉴权失败时的响应配置
     */
    private ErrorResponse errorResponse = new ErrorResponse();

    /**
     * 鉴权路由配置
     */
    @Data
    public static class AuthRoute {
        /**
         * 路径模式（Ant 风格，如 /api/**）
         */
        private String path;

        /**
         * 是否需要登录
         */
        private Boolean requireLogin = true;

        /**
         * 需要的角色（多个用逗号分隔）
         */
        private String requireRole;

        /**
         * 需要的权限（多个用逗号分隔）
         */
        private String requirePermission;

        /**
         * 多角色/权限的匹配模式
         * <p>ANY：拥有任一角色/权限即可（默认，向后兼容）</p>
         * <p>ALL：必须拥有全部角色/权限</p>
         */
        private MatchMode matchMode = MatchMode.ANY;
    }

    /**
     * 错误响应配置
     */
    @Data
    public static class ErrorResponse {
        /**
         * 未登录状态码
         */
        private Integer notLoginCode = 401;

        /**
         * 未登录提示消息
         */
        private String notLoginMessage = "请先登录";

        /**
         * 无权限状态码
         */
        private Integer noPermissionCode = 403;

        /**
         * 无权限提示消息
         */
        private String noPermissionMessage = "无访问权限";
    }

    /**
     * 角色/权限匹配模式
     */
    public enum MatchMode {
        /**
         * 拥有任一即可
         */
        ANY,
        /**
         * 必须全部拥有
         */
        ALL
    }
}
