package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hc.framework.common.model.Result;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;

import java.time.LocalDateTime;

/**
 * Sa-Token 网关错误响应构建器
 *
 * <p>提供统一的错误响应构建能力，供 Filter 和 ExceptionHandler 共用。</p>
 * <p>使用 Jackson 序列化 {@link com.hc.framework.common.model.Result}，
 * 与 hc-web-spring-boot-starter 的 Result 格式完全一致（含 timestamp 和 path 字段）。</p>
 * <p>错误码和错误消息可通过 {@code hc.satoken.gateway.error-response.*} 配置自定义。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class SaTokenGatewayErrorBuilder {

    /** 默认未登录状态码 */
    private static final int DEFAULT_NOT_LOGIN_CODE = 401;
    /** 默认无权限状态码 */
    private static final int DEFAULT_NO_PERMISSION_CODE = 403;
    /** 默认账号封禁状态码 */
    private static final int DEFAULT_ACCOUNT_DISABLED_CODE = 423;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final SaTokenGatewayProperties.ErrorResponse errorConfig;

    public SaTokenGatewayErrorBuilder(SaTokenGatewayProperties properties) {
        this.errorConfig = properties.getErrorResponse();
    }

    // ---- 公共 API ----

    /**
     * 构建错误响应 JSON
     */
    public String buildErrorJson(Throwable e) {
        return buildResultJson(getErrorCode(e), getErrorMessage(e));
    }

    /**
     * 构建错误响应 JSON（指定请求路径）
     */
    public String buildErrorJson(Throwable e, String path) {
        return buildResultJson(getErrorCode(e), getErrorMessage(e), path);
    }

    /**
     * 获取错误状态码（优先使用配置值，fallback 到默认值）
     */
    public int getErrorCode(Throwable e) {
        if (e instanceof NotLoginException) {
            return errorConfig.getNotLoginCode() != null
                    ? errorConfig.getNotLoginCode() : DEFAULT_NOT_LOGIN_CODE;
        }
        if (e instanceof NotPermissionException || e instanceof NotRoleException || e instanceof NotSafeException) {
            return DEFAULT_NO_PERMISSION_CODE;
        }
        if (e instanceof DisableServiceException) {
            return DEFAULT_ACCOUNT_DISABLED_CODE;
        }
        return 500;
    }

    /**
     * 获取错误消息（优先使用配置值，fallback 到内置中文消息）
     */
    public String getErrorMessage(Throwable e) {
        if (e instanceof NotLoginException) {
            return resolveNotLoginMessage((NotLoginException) e);
        }
        if (e instanceof NotPermissionException) {
            return "权限不足";
        }
        if (e instanceof NotRoleException) {
            return "角色权限不足";
        }
        if (e instanceof DisableServiceException) {
            return "账号已被封禁，请联系管理员";
        }
        if (e instanceof NotSafeException) {
            return "需要二次认证，请先完成安全验证";
        }
        return "认证服务异常";
    }

    /**
     * 构建 Result JSON 字符串
     */
    public String buildResultJson(int code, String message) {
        return buildResultJson(code, message, null);
    }

    /**
     * 构建 Result JSON 字符串（含请求路径）
     */
    public String buildResultJson(int code, String message, String path) {
        try {
            Result<Void> result = new Result<>();
            result.setCode(code);
            result.setMessage(message);
            result.setData(null);
            result.setTimestamp(LocalDateTime.now());
            if (path != null) {
                result.setPath(path);
            }
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            // 序列化失败时的兜底，使用手动构建的 JSON
            String escaped = escapeJson(message);
            return "{\"code\":" + code + ",\"message\":\"" + escaped + "\",\"data\":null}";
        }
    }

    // ---- 私有辅助方法 ----

    /**
     * 解析未登录详细消息
     */
    private String resolveNotLoginMessage(NotLoginException e) {
        // 如果配置了自定义消息，优先使用
        if (hasText(errorConfig.getNotLoginMessage())) {
            return errorConfig.getNotLoginMessage();
        }
        // 否则使用 Sa-Token 异常类型对应的内置消息
        return switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效";
            case NotLoginException.TOKEN_TIMEOUT -> "登录凭证已过期";
            case NotLoginException.BE_REPLACED -> "账号已在其他设备登录";
            case NotLoginException.KICK_OUT -> "账号已被踢下线";
            default -> "未登录或登录已过期";
        };
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
