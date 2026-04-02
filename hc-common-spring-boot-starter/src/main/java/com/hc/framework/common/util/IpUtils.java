package com.hc.framework.common.util;

import com.hc.framework.common.constant.HttpConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP 地址工具类
 *
 * <p>提供从 HTTP 请求中获取客户端真实 IP 地址的功能，支持多级代理场景。</p>
 *
 * <p>IP 获取优先级（处理反向代理/负载均衡场景）：</p>
 * <ol>
 *     <li>{@code X-Forwarded-For}：标准代理链头，取第一个非内网 IP</li>
 *     <li>{@code X-Real-IP}：Nginx 透传真实 IP</li>
 *     <li>{@code Proxy-Client-IP}：Apache HTTP 代理</li>
 *     <li>{@code WL-Proxy-Client-IP}：WebLogic 代理</li>
 *     <li>{@code HTTP_CLIENT_IP}：部分代理服务器</li>
 *     <li>{@code HTTP_X_FORWARDED_FOR}：部分代理服务器</li>
 *     <li>{@code request.getRemoteAddr()}：直连 IP（兜底）</li>
 * </ol>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 从当前请求上下文获取 IP（需在 Spring Web 环境中使用）
 * String ip = IpUtils.getClientIp();
 *
 * // 从指定请求对象获取 IP
 * String ip = IpUtils.getClientIp(request);
 *
 * // 判断是否内网 IP
 * boolean isLocal = IpUtils.isInternalIp("192.168.1.1");  // true
 * }</pre>
 *
 * @author hc-framework
 */
public class IpUtils {

    /** unknown 标识（代理头未设置时的默认值） */
    private static final String UNKNOWN = "unknown";

    /** IP 最大长度（IPv6 最长 39 位） */
    private static final int MAX_IP_LENGTH = 39;

    private IpUtils() {
    }

    /**
     * 从当前 Spring 请求上下文获取客户端 IP
     * <p>需在 Spring Web 环境（RequestContextHolder 有效）中调用，非 Web 线程中返回 null</p>
     *
     * @return 客户端 IP 地址，无法获取时返回 null
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return getClientIp(attributes.getRequest());
    }

    /**
     * 从指定 HttpServletRequest 中获取客户端真实 IP
     * <p>自动处理 X-Forwarded-For 多级代理场景，取第一个非 unknown 的 IP</p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(HttpConstants.HEADER_FORWARDED_FOR);
        if (isValidIp(ip)) {
            // X-Forwarded-For 可能包含多个 IP（正向代理链），取第一个
            int index = ip.indexOf(',');
            ip = index != -1 ? ip.substring(0, index).trim() : ip;
            return ip;
        }
        ip = request.getHeader(HttpConstants.HEADER_REAL_IP);
        if (isValidIp(ip)) {
            return ip;
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip;
        }
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip;
        }
        // 兜底：直接取 TCP 连接 IP（经过 NAT/代理后为代理 IP）
        ip = request.getRemoteAddr();
        // 本地回环地址统一返回 127.0.0.1
        if (HttpConstants.LOCAL_IPV6.equals(ip)) {
            return HttpConstants.LOCAL_IPV4;
        }
        return ip;
    }

    /**
     * 判断 IP 是否为内网地址（私有 IP 范围）
     *
     * <p>内网 IP 范围：</p>
     * <ul>
     *     <li>10.0.0.0 – 10.255.255.255</li>
     *     <li>172.16.0.0 – 172.31.255.255</li>
     *     <li>192.168.0.0 – 192.168.255.255</li>
     *     <li>127.0.0.1（本地回环）</li>
     * </ul>
     *
     * @param ip IPv4 地址字符串
     * @return true 表示内网地址
     */
    public static boolean isInternalIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        if (HttpConstants.LOCAL_IPV4.equals(ip) || HttpConstants.LOCAL_IPV6.equals(ip)) {
            return true;
        }
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            int first  = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            // 10.x.x.x
            if (first == 10) {
                return true;
            }
            // 172.16.x.x ~ 172.31.x.x
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            // 192.168.x.x
            return first == 192 && second == 168;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断是否为公网 IP
     *
     * @param ip IP 地址字符串
     * @return true 表示公网地址
     */
    public static boolean isPublicIp(String ip) {
        return !isInternalIp(ip);
    }

    /**
     * 判断字符串是否是有效的 IP（非 null、非 unknown、长度合理）
     *
     * @param ip IP 字符串
     * @return true 表示有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null
                && !ip.isBlank()
                && !UNKNOWN.equalsIgnoreCase(ip)
                && ip.length() <= MAX_IP_LENGTH;
    }
}
