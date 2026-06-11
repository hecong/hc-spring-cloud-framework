package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotSafeException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.hc.framework.common.model.Result;
import com.hc.framework.satoken.gateway.properties.SaTokenGatewayProperties;

import java.time.LocalDateTime;

/**
 * Sa-Token 网关错误响应构建器
 *
 * <p>提供统一的错误响应构建能力，供 Filter 和 ExceptionHandler 共用。</p>
 * <p>使用 Jackson 3.x 序列化 {@link com.hc.framework.common.model.Result}，
 * 与 hc-web-spring-boot-starter 的 Result 格式完全一致（含 timestamp 和 path 字段）。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class SaTokenGatewayErrorBuilder {

    private static final int DEFAULT_NOT_LOGIN_CODE = 401;
    private static final int DEFAULT_NO_PERMISSION_CODE = 403;
    private static final int DEFAULT_ACCOUNT_DISABLED_CODE = 423;

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final SaTokenGatewayProperties.ErrorResponse errorConfig;

    public SaTokenGatewayErrorBuilder(SaTokenGatewayProperties properties) {
        this.errorConfig = properties.getErrorResponse();
    }

    public String buildErrorJson(Throwable e) {
        return buildResultJson(getErrorCode(e), getErrorMessage(e));
    }

    public String buildErrorJson(Throwable e, String path) {
        return buildResultJson(getErrorCode(e), getErrorMessage(e), path);
    }

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

    public String buildResultJson(int code, String message) {
        return buildResultJson(code, message, null);
    }

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
            String escaped = escapeJson(message);
            return "{\"code\":" + code + ",\"message\":\"" + escaped + "\",\"data\":null}";
        }
    }

    private String resolveNotLoginMessage(NotLoginException e) {
        if (hasText(errorConfig.getNotLoginMessage())) {
            return errorConfig.getNotLoginMessage();
        }
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
