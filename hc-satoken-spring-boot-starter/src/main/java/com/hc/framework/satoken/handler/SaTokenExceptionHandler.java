package com.hc.framework.satoken.handler;

import cn.dev33.satoken.exception.*;
import com.hc.framework.web.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 全局异常处理器
 *
 * <p>统一处理 Sa-Token 相关异常，返回标准响应格式，并自动记录认证日志。</p>
 *
 * <p>异常码映射：</p>
 * <ul>
 *     <li>未登录 / Token 无效 → 401</li>
 *     <li>无权限 → 403</li>
 *     <li>账号被封禁 → 423</li>
 *     <li>其他认证异常 → 500</li>
 * </ul>
 *
 * <p>与 hc-logging-spring-boot-starter 集成，自动记录：</p>
 * <ul>
 *     <li>未登录访问日志</li>
 *     <li>权限校验失败日志</li>
 *     <li>角色校验失败日志</li>
 *     <li>账号封禁日志</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Order(-1)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    private final SaTokenAuthLogger authLogger;

    /**
     * 构造器注入
     *
     * @param authLogger 认证日志记录器
     */
    public SaTokenExceptionHandler(SaTokenAuthLogger authLogger) {
        this.authLogger = authLogger;
    }

    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("Sa-Token 未登录异常: type={}, message={}", e.getType(), e.getMessage());

        // 记录未登录日志
        authLogger.logNotLogin();

        String message = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效";
            case NotLoginException.TOKEN_TIMEOUT -> "登录凭证已过期";
            case NotLoginException.BE_REPLACED -> "账号已在其他设备登录";
            case NotLoginException.KICK_OUT -> "账号已被踢下线";
            default -> "未登录或登录已过期";
        };
        return Result.error(HttpStatus.UNAUTHORIZED.value(), message);
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("Sa-Token 无权限异常: permission={}", e.getPermission());

        // 记录权限校验失败日志
        authLogger.logPermissionDenied(getCurrentUserId(), e.getPermission());

        return Result.error(HttpStatus.FORBIDDEN.value(), "权限不足");
    }

    /**
     * 处理无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("Sa-Token 无角色异常: role={}", e.getRole());

        // 记录角色校验失败日志
        authLogger.logRoleDenied(getCurrentUserId(), e.getRole());

        return Result.error(HttpStatus.FORBIDDEN.value(), "角色权限不足");
    }

    /**
     * 处理账号被封禁异常
     */
    @ExceptionHandler(DisableServiceException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public Result<Void> handleDisableServiceException(DisableServiceException e) {
        log.warn("Sa-Token 账号封禁异常: service={}, level={}, time={}",
                e.getService(), e.getLevel(), e.getDisableTime());

        // 记录账号封禁日志
        authLogger.logAccountDisabled(getCurrentUserId(), "账号已被封禁，等级：" + e.getLevel());

        return Result.error(HttpStatus.LOCKED.value(), "账号已被封禁，请联系管理员");
    }

    /**
     * 处理二级认证失败异常
     */
    @ExceptionHandler(NotSafeException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotSafeException(NotSafeException e) {
        log.warn("Sa-Token 二级认证失败: service={}", e.getService());
        return Result.error(HttpStatus.FORBIDDEN.value(), "需要二次认证，请先完成安全验证");
    }

    /**
     * 处理其他 Sa-Token 异常
     */
    @ExceptionHandler(SaTokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSaTokenException(SaTokenException e) {
        log.error("Sa-Token 认证异常: {}", e.getMessage(), e);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "认证服务异常");
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        try {
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            if (loginId instanceof Long) {
                return (Long) loginId;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
