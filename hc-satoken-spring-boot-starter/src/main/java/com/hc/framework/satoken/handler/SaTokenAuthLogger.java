package com.hc.framework.satoken.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.common.util.IpUtils;
import com.hc.framework.satoken.config.SaTokenProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sa-Token 认证日志记录器
 *
 * <p>自动记录登录成功/失败、权限校验失败等认证相关日志。</p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     auth-log:
 *       login-log-enabled: true
 *       auth-log-enabled: true
 *       log-login-success: true
 *       log-login-failure: true
 *       log-permission-denied: true
 *       log-format: DETAIL  # SIMPLE 或 DETAIL
 * }</pre>
 *
 * <p>日志输出示例：</p>
 * <pre>
 * [AUTH_LOGIN_SUCCESS] userId=1, username=admin, ip=192.168.1.1, time=2024-01-01 12:00:00
 * [AUTH_LOGIN_FAILURE] username=admin, ip=192.168.1.1, reason=密码错误, time=2024-01-01 12:00:00
 * [AUTH_PERMISSION_DENIED] userId=1, permission=user:delete, ip=192.168.1.1, time=2024-01-01 12:00:00
 * </pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenAuthLogger {

    private final SaTokenProperties saTokenProperties;

    /**
     * 构造器注入
     *
     * @param saTokenProperties Sa-Token 配置属性
     */
    public SaTokenAuthLogger(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 登录日志 ====================

    /**
     * 记录登录成功日志
     *
     * @param userId   用户 ID
     * @param username 用户名
     */
    public void logLoginSuccess(Long userId, String username) {
        if (!isLoginLogEnabled() || !isLogLoginSuccessEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
        String time = LocalDateTime.now().format(TIME_FORMATTER);

        if (isDetailFormat()) {
            log.info("[AUTH_LOGIN_SUCCESS] userId={}, username={}, ip={}, time={}, userAgent={}",
                    userId, username, ip, time, request != null ? request.getHeader("User-Agent") : "unknown");
        } else {
            log.info("[AUTH_LOGIN_SUCCESS] userId={}, username={}, ip={}", userId, username, ip);
        }
    }

    /**
     * 记录登录失败日志
     *
     * @param username 用户名
     * @param reason   失败原因
     */
    public void logLoginFailure(String username, String reason) {
        if (!isLoginLogEnabled() || !isLogLoginFailureEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
        String time = LocalDateTime.now().format(TIME_FORMATTER);

        if (isDetailFormat()) {
            log.warn("[AUTH_LOGIN_FAILURE] username={}, ip={}, reason={}, time={}, userAgent={}",
                    username, ip, reason, time, request != null ? request.getHeader("User-Agent") : "unknown");
        } else {
            log.warn("[AUTH_LOGIN_FAILURE] username={}, ip={}, reason={}", username, ip, reason);
        }
    }

    /**
     * 记录登出日志
     *
     * @param userId 用户 ID
     */
    public void logLogout(Long userId) {
        if (!isLoginLogEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";

        log.info("[AUTH_LOGOUT] userId={}, ip={}", userId, ip);
    }

    /**
     * 记录被踢下线日志
     *
     * @param userId 用户 ID
     */
    public void logKickout(Long userId) {
        if (!isLoginLogEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";

        log.warn("[AUTH_KICKOUT] userId={}, ip={}", userId, ip);
    }

    /**
     * 记录被顶下线日志
     *
     * @param userId 用户 ID
     */
    public void logReplaced(Long userId) {
        if (!isLoginLogEnabled()) {
            return;
        }

        log.warn("[AUTH_REPLACED] userId={} 在其他设备登录", userId);
    }

    // ==================== 权限日志 ====================

    /**
     * 记录权限校验失败日志
     *
     * @param userId     用户 ID
     * @param permission 缺少的权限
     */
    public void logPermissionDenied(Long userId, String permission) {
        if (!isAuthLogEnabled() || !isLogPermissionDeniedEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
        String uri = request != null ? request.getRequestURI() : "unknown";
        String time = LocalDateTime.now().format(TIME_FORMATTER);

        if (isDetailFormat()) {
            log.warn("[AUTH_PERMISSION_DENIED] userId={}, permission={}, ip={}, uri={}, time={}",
                    userId, permission, ip, uri, time);
        } else {
            log.warn("[AUTH_PERMISSION_DENIED] userId={}, permission={}, uri={}", userId, permission, uri);
        }
    }

    /**
     * 记录角色校验失败日志
     *
     * @param userId 用户 ID
     * @param role   缺少的角色
     */
    public void logRoleDenied(Long userId, String role) {
        if (!isAuthLogEnabled() || !isLogPermissionDeniedEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
        String uri = request != null ? request.getRequestURI() : "unknown";

        log.warn("[AUTH_ROLE_DENIED] userId={}, role={}, ip={}, uri={}", userId, role, ip, uri);
    }

    /**
     * 记录未登录访问日志
     */
    public void logNotLogin() {
        if (!isAuthLogEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
        String uri = request != null ? request.getRequestURI() : "unknown";

        log.warn("[AUTH_NOT_LOGIN] ip={}, uri={}", ip, uri);
    }

    /**
     * 记录账号被封禁日志
     *
     * @param userId 用户 ID
     * @param reason 封禁原因
     */
    public void logAccountDisabled(Long userId, String reason) {
        if (!isAuthLogEnabled()) {
            return;
        }

        HttpServletRequest request = getRequest();
        String ip = request != null ? IpUtils.getClientIp(request) : "unknown";

        log.error("[AUTH_ACCOUNT_DISABLED] userId={}, reason={}, ip={}", userId, reason, ip);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            if (loginId instanceof Long) {
                return (Long) loginId;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否启用登录日志
     */
    private boolean isLoginLogEnabled() {
        return Boolean.TRUE.equals(saTokenProperties.getAuthLog().getLoginLogEnabled());
    }

    /**
     * 是否启用鉴权日志
     */
    private boolean isAuthLogEnabled() {
        return Boolean.TRUE.equals(saTokenProperties.getAuthLog().getAuthLogEnabled());
    }

    /**
     * 是否记录登录成功日志
     */
    private boolean isLogLoginSuccessEnabled() {
        return Boolean.TRUE.equals(saTokenProperties.getAuthLog().getLogLoginSuccess());
    }

    /**
     * 是否记录登录失败日志
     */
    private boolean isLogLoginFailureEnabled() {
        return Boolean.TRUE.equals(saTokenProperties.getAuthLog().getLogLoginFailure());
    }

    /**
     * 是否记录权限校验失败日志
     */
    private boolean isLogPermissionDeniedEnabled() {
        return Boolean.TRUE.equals(saTokenProperties.getAuthLog().getLogPermissionDenied());
    }

    /**
     * 是否详细格式
     */
    private boolean isDetailFormat() {
        return "DETAIL".equalsIgnoreCase(saTokenProperties.getAuthLog().getLogFormat());
    }
}
