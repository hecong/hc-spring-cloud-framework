package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.exception.*;
import com.hc.framework.satoken.gateway.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Sa-Token 网关全局异常处理器
 *
 * <p>统一处理 Sa-Token 相关异常，转换为框架统一的 Result 响应格式。</p>
 * <p>支持自定义字段名（通过 hc.web.code-field 等配置）。</p>
 *
 * <p>异常码映射：</p>
 * <ul>
 *     <li>未登录 / Token 无效 → 401</li>
 *     <li>无权限 → 403</li>
 *     <li>账号被封禁 → 423</li>
 *     <li>其他认证异常 → 500</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Order(-1)  // 高优先级，确保在默认异常处理器之前执行
public class SaTokenGatewayExceptionHandler implements WebExceptionHandler {

    /**
     * 未登录状态码
     */
    private static final int NOT_LOGIN_CODE = 401;

    /**
     * 无权限状态码
     */
    private static final int NO_PERMISSION_CODE = 403;

    /**
     * 账号被封禁状态码
     */
    private static final int ACCOUNT_DISABLED_CODE = 423;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 只处理 Sa-Token 相关异常
        if (!(ex instanceof SaTokenException)) {
            return Mono.error(ex);
        }

        log.warn("Sa-Token 网关异常: {}", ex.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = convertToResult(ex);

        // 设置 HTTP 状态码
        response.setStatusCode(HttpStatus.valueOf(result.getCode()));

        // 写入响应
        String json = convertResultToJson(result);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * 将 Sa-Token 异常转换为 Result
     *
     * @param ex 异常对象
     * @return Result 对象
     */
    private Result<Void> convertToResult(Throwable ex) {
        if (ex instanceof NotLoginException) {
            return createErrorResult(NOT_LOGIN_CODE, getNotLoginMessage((NotLoginException) ex));
        }

        if (ex instanceof NotPermissionException) {
            NotPermissionException e = (NotPermissionException) ex;
            return createErrorResult(NO_PERMISSION_CODE, "无操作权限: " + e.getPermission());
        }

        if (ex instanceof NotRoleException) {
            NotRoleException e = (NotRoleException) ex;
            return createErrorResult(NO_PERMISSION_CODE, "无访问权限，需要角色: " + e.getRole());
        }

        if (ex instanceof DisableServiceException) {
            return createErrorResult(ACCOUNT_DISABLED_CODE, "账号已被封禁，请联系管理员");
        }

        if (ex instanceof NotSafeException) {
            return createErrorResult(NO_PERMISSION_CODE, "需要二次认证，请先完成安全验证");
        }

        // 其他 Sa-Token 异常
        return createErrorResult(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                "认证服务异常: " + ex.getMessage());
    }

    /**
     * 获取未登录的详细消息
     *
     * @param e NotLoginException
     * @return 消息
     */
    private String getNotLoginMessage(NotLoginException e) {
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
     * 创建错误 Result
     *
     * @param code    状态码
     * @param message 消息
     * @return Result
     */
    private Result<Void> createErrorResult(int code, String message) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 将 Result 转换为 JSON 字符串
     *
     * <p>使用 Jackson 手动序列化，确保与框架的 ResultSerializer 逻辑一致。</p>
     *
     * @param result Result 对象
     * @return JSON 字符串
     */
    private String convertResultToJson(Result<Void> result) {
        // 使用简单的 JSON 构建，确保与框架 Result 格式一致
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"code\":").append(result.getCode()).append(",");
        json.append("\"message\":\"").append(escapeJson(result.getMessage())).append("\"");

        json.append(",\"data\":null");

        if (result.getTimestamp() != null) {
            json.append(",\"timestamp\":\"").append(result.getTimestamp()).append("\"");
        }
        
        if (result.getPath() != null) {
            json.append(",\"path\":\"").append(escapeJson(result.getPath())).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String str) {
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
