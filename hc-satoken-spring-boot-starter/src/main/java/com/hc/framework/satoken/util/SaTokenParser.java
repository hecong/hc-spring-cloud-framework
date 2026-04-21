package com.hc.framework.satoken.util;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.satoken.config.SaTokenProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Sa-Token Token 解析器
 *
 * <p>支持从 Header、Parameter、Cookie 中自动解析 Token，优先级可配置。</p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     frontend:
 *       token-read-order: HEADER,PARAMETER,COOKIE
 * }</pre>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 获取当前请求的 Token
 * String token = SaTokenParser.getToken(request);
 *
 * // 获取当前登录用户 ID
 * Long userId = SaTokenParser.getLoginUserId();
 *
 * // 判断是否已登录
 * boolean isLogin = SaTokenParser.isLogin();
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SaTokenParser {

    private final SaTokenProperties saTokenProperties;

    /**
     * Token 来源枚举
     */
    public enum TokenSource {
        /**
         * 请求头
         */
        HEADER,
        /**
         * 请求参数
         */
        PARAMETER,
        /**
         * Cookie
         */
        COOKIE
    }

    /**
     * 从请求中获取 Token
     *
     * @param request HTTP 请求
     * @return Token 值，未找到返回 null
     */
    public String getToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String tokenName = saTokenProperties.getToken().getName();
        List<String> readOrder = getTokenReadOrder();

        for (String source : readOrder) {
            String token = switch (source.toUpperCase()) {
                case "HEADER" -> getTokenFromHeader(request, tokenName);
                case "PARAMETER" -> getTokenFromParameter(request, tokenName);
                case "COOKIE" -> getTokenFromCookie(request, tokenName);
                default -> null;
            };

            if (token != null && !token.isEmpty()) {
                log.debug("Token 从 {} 中获取成功", source);
                return token;
            }
        }

        return null;
    }

    /**
     * 获取当前请求的 Token（从 Sa-Token 上下文）
     *
     * @return Token 值
     */
    public String getToken() {
        return StpUtil.getTokenValue();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，未登录返回 null
     */
    public Long getLoginUserId() {
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
            log.warn("获取登录用户 ID 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前登录用户 ID（带默认值）
     *
     * @param defaultValue 默认值
     * @return 用户 ID
     */
    public Long getLoginUserId(Long defaultValue) {
        Long userId = getLoginUserId();
        return userId != null ? userId : defaultValue;
    }

    /**
     * 判断当前用户是否已登录
     *
     * @return 是否已登录
     */
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 获取 Token 来源
     *
     * @param request HTTP 请求
     * @return Token 来源
     */
    public TokenSource getTokenSource(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String tokenName = saTokenProperties.getToken().getName();
        List<String> readOrder = getTokenReadOrder();

        for (String source : readOrder) {
            String token = switch (source.toUpperCase()) {
                case "HEADER" -> getTokenFromHeader(request, tokenName);
                case "PARAMETER" -> getTokenFromParameter(request, tokenName);
                case "COOKIE" -> getTokenFromCookie(request, tokenName);
                default -> null;
            };

            if (token != null && !token.isEmpty()) {
                return TokenSource.valueOf(source.toUpperCase());
            }
        }

        return null;
    }

    /**
     * 从请求头获取 Token
     *
     * @param request   HTTP 请求
     * @param tokenName Token 名称
     * @return Token 值
     */
    private String getTokenFromHeader(HttpServletRequest request, String tokenName) {
        String token = request.getHeader(tokenName);
        if (token != null && !token.isEmpty()) {
            // 处理 Bearer 前缀
            String prefix = saTokenProperties.getToken().getPrefix();
            if (prefix != null && !prefix.isEmpty() && token.startsWith(prefix)) {
                return token.substring(prefix.length()).trim();
            }
            return token;
        }
        return null;
    }

    /**
     * 从请求参数获取 Token
     *
     * @param request   HTTP 请求
     * @param tokenName Token 名称
     * @return Token 值
     */
    private String getTokenFromParameter(HttpServletRequest request, String tokenName) {
        String token = request.getParameter(tokenName);
        if (token != null && !token.isEmpty()) {
            log.debug("从 URL 参数读取到 Token，注意：Token 可能泄露到日志/Referer/浏览器历史中");
            return token;
        }
        return null;
    }

    /**
     * 从 Cookie 获取 Token
     *
     * @param request   HTTP 请求
     * @param tokenName Token 名称
     * @return Token 值
     */
    private String getTokenFromCookie(HttpServletRequest request, String tokenName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> tokenName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取 Token 读取优先级列表
     */
    private List<String> getTokenReadOrder() {
        String order = saTokenProperties.getFrontend().getTokenReadOrder();
        return Arrays.asList(order.split(","));
    }

    /**
     * 构建 Token 响应头（用于登录后返回 Token）
     *
     * @param token Token 值
     * @return 完整的 Token 响应头值（带前缀）
     */
    public String buildTokenHeader(String token) {
        String prefix = saTokenProperties.getToken().getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + " " + token;
        }
        return token;
    }
}
