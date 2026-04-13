package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.exception.*;

/**
 * Sa-Token 网关错误响应构建器
 *
 * <p>提供统一的错误响应构建能力，供 Filter 和 ExceptionHandler 共用。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class SaTokenGatewayErrorBuilder {

    /**
     * 未登录状态码
     */
    public static final int NOT_LOGIN_CODE = 401;

    /**
     * 无权限状态码
     */
    public static final int NO_PERMISSION_CODE = 403;

    /**
     * 账号被封禁状态码
     */
    public static final int ACCOUNT_DISABLED_CODE = 423;

    /**
     * 构建错误响应 JSON
     *
     * @param e 异常对象
     * @return JSON 字符串
     */
    public static String buildErrorJson(Throwable e) {
        int code = getErrorCode(e);
        String message = getErrorMessage(e);
        return buildResultJson(code, message);
    }

    /**
     * 获取错误状态码
     *
     * @param e 异常对象
     * @return 状态码
     */
    public static int getErrorCode(Throwable e) {
        if (e instanceof NotLoginException) {
            return NOT_LOGIN_CODE;
        }
        if (e instanceof NotPermissionException || e instanceof NotRoleException || e instanceof NotSafeException) {
            return NO_PERMISSION_CODE;
        }
        if (e instanceof DisableServiceException) {
            return ACCOUNT_DISABLED_CODE;
        }
        return 500;
    }

    /**
     * 获取错误消息
     *
     * @param e 异常对象
     * @return 消息
     */
    public static String getErrorMessage(Throwable e) {
        if (e instanceof NotLoginException) {
            return getNotLoginMessage((NotLoginException) e);
        }
        if (e instanceof NotPermissionException npe) {
            return "无操作权限: " + npe.getPermission();
        }
        if (e instanceof NotRoleException nre) {
            return "无访问权限，需要角色: " + nre.getRole();
        }
        if (e instanceof DisableServiceException) {
            return "账号已被封禁，请联系管理员";
        }
        if (e instanceof NotSafeException) {
            return "需要二次认证，请先完成安全验证";
        }
        return "认证服务异常: " + e.getMessage();
    }

    /**
     * 获取未登录的详细消息
     */
    private static String getNotLoginMessage(NotLoginException e) {
        return switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效";
            case NotLoginException.TOKEN_TIMEOUT -> "登录凭证已过期";
            case NotLoginException.BE_REPLACED -> "账号已在其他设备登录";
            case NotLoginException.KICK_OUT -> "账号已被踢下线";
            default -> "未登录或登录已过期";
        };
    }

    /**
     * 构建 Result JSON 字符串
     *
     * @param code    状态码
     * @param message 消息
     * @return JSON 字符串
     */
    public static String buildResultJson(int code, String message) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"code\":").append(code).append(",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\"");
        json.append(",\"data\":null");
        json.append("}");
        return json.toString();
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
