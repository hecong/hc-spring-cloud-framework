package com.hc.framework.satoken.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 网关配置属性
 *
 * <p>网关只做 Token 认证（登录校验），不做角色/权限校验。
 * 权限控制全部下沉到微服务层通过注解实现。</p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     gateway:
 *       enabled: true
 *       exclude-paths:
 *         - /api/auth/login
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
     * 排除路径列表（支持 Ant 风格通配符，这些路径不校验登录）
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
    }
}
