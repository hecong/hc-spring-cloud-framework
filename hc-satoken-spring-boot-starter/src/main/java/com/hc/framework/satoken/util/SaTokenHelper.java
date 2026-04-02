package com.hc.framework.satoken.util;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.config.SaTokenProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Sa-Token 操作工具类
 *
 * <p>封装常用的 Token 操作方法，简化业务代码编写。</p>
 *
 * <p>提供以下核心功能：</p>
 * <ul>
 *   <li>登录认证：登录、注销、强制下线</li>
 *   <li>Token 信息：获取当前用户ID、Token值</li>
 *   <li>权限校验：角色、权限检查</li>
 *   <li>会话管理：踢人下线、禁用账号</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private SaTokenHelper saTokenHelper;
 *
 * // 登录
 * saTokenHelper.login(userId);
 *
 * // 获取当前用户ID
 * Long userId = saTokenHelper.getCurrentUserId();
 *
 * // 检查权限
 * boolean hasPerm = saTokenHelper.hasPermission("user:add");
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaTokenHelper {

    private final SaTokenProperties saTokenProperties;

    /**
     * 构造器注入配置属性
     */
    public SaTokenHelper(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    // ==================== 登录认证 ====================

    /**
     * 用户登录
     *
     * @param userId 用户ID
     */
    public void login(Long userId) {
        StpUtil.login(userId);
        log.info("用户登录成功: userId={}", userId);
    }

    /**
     * 用户登录（带设备标识）
     *
     * @param userId   用户ID
     * @param deviceId 设备标识（用于多端登录控制）
     */
    public void login(Long userId, String deviceId) {
        StpUtil.login(userId, deviceId);
        log.info("用户登录成功: userId={}, deviceId={}", userId, deviceId);
    }

    /**
     * 用户登录（记住我模式）
     *
     * @param userId     用户ID
     * @param rememberMe 是否记住我
     */
    public void login(Long userId, boolean rememberMe) {
        if (rememberMe) {
            StpUtil.login(userId, true);
        } else {
            StpUtil.login(userId);
        }
        log.info("用户登录成功: userId={}, rememberMe={}", userId, rememberMe);
    }

    /**
     * 用户注销登录
     */
    public void logout() {
        if (isLogin()) {
            Long userId = getCurrentUserId();
            StpUtil.logout();
            log.info("用户注销成功: userId={}", userId);
        }
    }

    /**
     * 强制指定用户下线
     *
     * @param userId 用户ID
     */
    public void kickout(Long userId) {
        StpUtil.kickout(userId);
        log.info("用户被强制下线: userId={}", userId);
    }

    /**
     * 强制指定用户下线（指定设备）
     *
     * @param userId   用户ID
     * @param deviceId 设备标识
     */
    public void kickout(Long userId, String deviceId) {
        StpUtil.kickout(userId, deviceId);
        log.info("用户被强制下线: userId={}, deviceId={}", userId, deviceId);
    }

    // ==================== 登录状态 ====================

    /**
     * 判断当前用户是否已登录
     *
     * @return true=已登录, false=未登录
     */
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     * @throws cn.dev33.satoken.exception.NotLoginException 未登录时抛出异常
     */
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户ID（未登录返回null）
     *
     * @return 用户ID，未登录时返回null
     */
    public Long getCurrentUserIdOrNull() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前 Token 值
     *
     * @return Token字符串
     */
    public String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 获取 Token 有效期（秒）
     *
     * @return 有效期，-1表示永不过期
     */
    public long getTokenTimeout() {
        return StpUtil.getTokenTimeout();
    }

    // ==================== 权限校验 ====================

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param role 角色标识
     * @return true=有角色, false=无角色
     */
    public boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * 判断当前用户是否拥有指定角色（任意一个）
     *
     * @param roles 角色标识数组
     * @return true=有任意一个角色, false=无任何角色
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (StpUtil.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否拥有指定角色
     *
     * @param role 角色标识
     * @throws cn.dev33.satoken.exception.NotRoleException 无角色时抛出异常
     */
    public void checkRole(String role) {
        StpUtil.checkRole(role);
    }

    /**
     * 判断当前用户是否拥有指定权限
     *
     * @param permission 权限标识
     * @return true=有权限, false=无权限
     */
    public boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    /**
     * 判断当前用户是否拥有指定权限（任意一个）
     *
     * @param permissions 权限标识数组
     * @return true=有任意一个权限, false=无任何权限
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (StpUtil.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否拥有指定权限
     *
     * @param permission 权限标识
     * @throws cn.dev33.satoken.exception.NotPermissionException 无权限时抛出异常
     */
    public void checkPermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    /**
     * 判断权限校验是否启用
     *
     * @return true=启用, false=禁用
     */
    public boolean isPermissionEnabled() {
        return saTokenProperties.getPermission().getEnabled();
    }

    // ==================== 会话管理 ====================

    /**
     * 禁用指定用户账号
     *
     * @param userId 用户ID
     * @param time   禁用时长（秒）
     */
    public void disable(Long userId, long time) {
        StpUtil.disable(userId, time);
        log.warn("账号已被禁用: userId={}, time={}s", userId, time);
    }

    /**
     * 解除用户禁用
     *
     * @param userId 用户ID
     */
    public void untieDisable(Long userId) {
        StpUtil.untieDisable(userId);
        log.info("账号已解禁: userId={}", userId);
    }

    /**
     * 判断用户是否被禁用
     *
     * @param userId 用户ID
     * @return true=已禁用, false=正常
     */
    public boolean isDisable(Long userId) {
        return StpUtil.isDisable(userId);
    }

    /**
     * 获取用户禁用剩余时间（秒）
     *
     * @param userId 用户ID
     * @return 剩余时间，-1表示未禁用
     */
    public long getDisableTime(Long userId) {
        return StpUtil.getDisableTime(userId);
    }

    // ==================== Token 信息 ====================

    /**
     * 获取用户 Token 值
     *
     * @param userId 用户ID
     * @return Token值
     */
    public String getTokenValue(Long userId) {
        return StpUtil.getTokenValueByLoginId(userId);
    }

    /**
     * 获取用户 Session
     *
     * @param userId 用户ID
     * @return Session 对象
     */
    public cn.dev33.satoken.session.SaSession getSession(Long userId) {
        return StpUtil.getSessionByLoginId(userId);
    }

    /**
     * 刷新当前 Token 有效期
     */
    public void refreshToken() {
        if (isLogin()) {
            StpUtil.renewTimeout(saTokenProperties.getToken().getTimeout());
        }
    }

    /**
     * 获取 Token 名称
     *
     * @return Token名称
     */
    public String getTokenName() {
        return saTokenProperties.getToken().getName();
    }

    /**
     * 获取 Token 前缀
     *
     * @return Token前缀
     */
    public String getTokenPrefix() {
        return saTokenProperties.getToken().getPrefix();
    }

    // ==================== 工具方法 ====================

    /**
     * 获取配置属性（供业务项目使用）
     *
     * @return SaTokenProperties
     */
    public SaTokenProperties getProperties() {
        return saTokenProperties;
    }

    /**
     * 获取所有排除路径
     *
     * @return 排除路径列表
     */
    public List<String> getExcludePaths() {
        return saTokenProperties.getAllExcludePaths();
    }
}
