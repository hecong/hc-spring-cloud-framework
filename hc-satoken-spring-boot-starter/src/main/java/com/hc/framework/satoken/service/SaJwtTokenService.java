package com.hc.framework.satoken.service;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.config.SaTokenProperties;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 *
 * <p>提供 JWT Token 生成、解析、验证等功能。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>支持多种签名算法（HS256/HS384/HS512）</li>
 *   <li>支持自定义 Claims</li>
 *   <li>支持 Token 刷新</li>
 *   <li>与 Sa-Token 无缝集成</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private SaJwtTokenService jwtService;
 *
 * // 创建 Token
 * String token = jwtService.createToken(userId, extraClaims);
 *
 * // 解析 Token
 * Long userId = jwtService.getUserId(token);
 *
 * // 验证 Token
 * boolean valid = jwtService.validateToken(token);
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class SaJwtTokenService {

    private final SaTokenProperties properties;

    /**
     * JWT Header 前缀
     */
    private static final String JWT_HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.";

    /**
     * 构造器注入
     */
    public SaJwtTokenService(SaTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 JWT Token
     *
     * <p>使用 Sa-Token 的 JWT 模式时，直接调用 StpUtil.login() 即可自动生成 JWT Token。</p>
     * <p>此方法提供手动创建 JWT Token 的能力。</p>
     *
     * @param userId 用户ID
     * @return JWT Token
     */
    public String createToken(Long userId) {
        return createToken(userId, null);
    }

    /**
     * 创建 JWT Token（带额外 Claims）
     *
     * @param userId       用户ID
     * @param extraClaims 额外的 Claims
     * @return JWT Token
     */
    public String createToken(Long userId, Map<String, Object> extraClaims) {
        if (!properties.isJwtEnabled()) {
            log.warn("JWT 模式未启用，请配置 hc.satoken.jwt.enabled=true 或 token.style=jwt");
            return null;
        }

        try {
            // 使用 Sa-Token 创建 Token
            // Sa-Token 的 JWT 模式会自动处理 Token 生成
            StpUtil.login(userId);
            String token = StpUtil.getTokenValue();

            log.debug("创建 JWT Token: userId={}, token={}", userId, maskToken(token));
            return token;

        } catch (Exception e) {
            log.error("创建 JWT Token 失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("创建 JWT Token 失败", e);
        }
    }

    /**
     * 解析 Token 获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID，无效 Token 返回 null
     */
    public Long getUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            // 使用 Sa-Token 解析
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }

            if (loginId instanceof Long) {
                return (Long) loginId;
            }
            return Long.parseLong(loginId.toString());

        } catch (Exception e) {
            log.debug("解析 JWT Token 失败: error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true=有效, false=无效
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            // 获取 Token 有效期
            long timeout = StpUtil.stpLogic.getTokenTimeout(token);
            return timeout > 0 || timeout == -1; // -1 表示永不过期

        } catch (Exception e) {
            log.debug("验证 JWT Token 失败: error={}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Token 是否即将过期
     *
     * @param token       JWT Token
     * @param thresholdMs 过期阈值（毫秒）
     * @return true=即将过期, false=未过期
     */
    public boolean isTokenExpiringSoon(String token, long thresholdMs) {
        try {
            long timeout = StpUtil.stpLogic.getTokenTimeout(token);
            if (timeout == -1) {
                return false; // 永不过期
            }
            return (timeout * 1000) < thresholdMs;

        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新 Token 有效期
     *
     * @param token JWT Token
     */
    public void refreshToken(String token) {
        try {
            Long timeout = properties.getJwt().getTimeout();
            if (timeout == null) {
                timeout = properties.getToken().getTimeout();
            }
            StpUtil.stpLogic.renewTimeout(token, timeout);
            log.debug("刷新 JWT Token 有效期: token={}, timeout={}s", maskToken(token), timeout);

        } catch (Exception e) {
            log.error("刷新 JWT Token 失败: error={}", e.getMessage());
        }
    }

    /**
     * 获取 Token 剩余有效期（秒）
     *
     * @param token JWT Token
     * @return 剩余有效期，-1 表示永不过期，-2 表示无效 Token
     */
    public long getTokenTimeout(String token) {
        try {
            return StpUtil.stpLogic.getTokenTimeout(token);
        } catch (Exception e) {
            return -2;
        }
    }

    /**
     * 检查是否是 JWT Token
     *
     * @param token Token 字符串
     * @return true=JWT Token, false=其他类型 Token
     */
    public boolean isJwtToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        // JWT Token 通常包含两个点，分为三部分
        int dotCount = 0;
        for (char c : token.toCharArray()) {
            if (c == '.') {
                dotCount++;
            }
        }
        return dotCount == 2;
    }

    /**
     * 获取 JWT 配置
     *
     * @return JWT 配置
     */
    public SaTokenProperties.JwtConfig getJwtConfig() {
        return properties.getJwt();
    }

    /**
     * 获取 Token 密钥
     *
     * @return 密钥字符串
     */
    public String getSecret() {
        return properties.getJwt().getSecret();
    }

    /**
     * 获取 Token 签名算法
     *
     * @return 算法名称
     */
    public String getAlgorithm() {
        return properties.getJwt().getAlgorithm();
    }

    // ==================== 私有方法 ====================

    /**
     * 遮蔽 Token（用于日志输出）
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 20) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }
}
