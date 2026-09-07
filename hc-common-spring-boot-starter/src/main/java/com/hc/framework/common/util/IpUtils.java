package com.hc.framework.common.util;

import com.hc.framework.common.constant.HttpConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * IP 地址工具类
 *
 * <p>提供从 HTTP 请求中获取客户端真实 IP 地址的功能，支持可信代理链（精确 IP + IPv4 CIDR）。</p>
 *
 * <p><b>安全语义（自 1.1.0）：</b>缺省不信任任何代理头——未配置可信代理列表时，
 * 直接取 TCP 对端地址 {@code remoteAddr}（{@code ::1} 归一为 {@code 127.0.0.1}），
 * 攻击者伪造的 {@code X-Forwarded-For}/{@code X-Real-IP} 不再被采信；仅当
 * {@code remoteAddr} 为本地回环（本机 Nginx 单跳/调试）时才优先采用这两个头。</p>
 *
 * <p>配置可信代理列表（如 {@code hc.web.trusted-proxies=10.0.0.0/8,192.168.1.1}）后：
 * 对来自可信代理的请求，从 {@code X-Forwarded-For} 右向左跳过可信代理，返回首个不可信 IP；
 * 链中全可信/无该头时回退 {@code X-Real-IP}，再回退 {@code remoteAddr}；不可信直连忽略全部转发头。</p>
 *
 * <p>限制：IPv6 仅支持精确匹配（不做 CIDR 位运算）；XFF 畸形值不做深度清洗（basic trim + 合法性校验）。</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 从当前请求上下文获取 IP（需在 Spring Web 环境中使用）
 * String ip = IpUtils.getClientIp();
 *
 * // 从指定请求对象获取 IP（走启动注入的可信代理配置）
 * String ip = IpUtils.getClientIp(request);
 *
 * // 显式指定可信代理列表
 * String ip = IpUtils.getClientIp(request, List.of("10.0.0.0/8"));
 * }</pre>
 *
 * @author hc-framework
 */
public class IpUtils {

    /** unknown 标识（代理头未设置时的默认值） */
    private static final String UNKNOWN = "unknown";

    /** IPv6 文本最大长度 */
    private static final int MAX_IPV6_LENGTH = 45;

    /**
     * 可信代理配置持有者（启动期由 web starter 注入），默认空 = 不信任任何代理头
     */
    private static volatile List<String> trustedProxies = Collections.emptyList();

    private IpUtils() {
    }

    // ==================== 可信代理配置 ====================

    /**
     * 注入可信代理列表（null 视为清空；内容做 trim 拷贝，避免外部可变）
     */
    public static void setTrustedProxies(List<String> proxies) {
        if (proxies == null || proxies.isEmpty()) {
            trustedProxies = Collections.emptyList();
            return;
        }
        List<String> normalized = new ArrayList<>(proxies.size());
        for (String proxy : proxies) {
            if (proxy != null && !proxy.trim().isEmpty()) {
                normalized.add(proxy.trim());
            }
        }
        trustedProxies = Collections.unmodifiableList(normalized);
    }

    /**
     * 注入可信代理列表（可变参数形式）
     */
    public static void setTrustedProxies(String... proxies) {
        setTrustedProxies(proxies == null ? Collections.emptyList() : List.of(proxies));
    }

    // ==================== 客户端 IP 解析 ====================

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
     * 从指定 HttpServletRequest 中获取客户端真实 IP（走启动注入的可信代理配置，缺省空=安全默认）
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        return resolveClientIp(request, trustedProxies);
    }

    /**
     * 从指定 HttpServletRequest 中获取客户端真实 IP（显式指定可信代理列表）
     *
     * @param request         HTTP 请求
     * @param trustedProxies 可信代理列表（精确 IP / IPv4 CIDR），null 等价空列表
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request, List<String> trustedProxies) {
        return resolveClientIp(request, trustedProxies == null ? Collections.emptyList() : trustedProxies);
    }

    /**
     * 解析算法：
     * <ol>
     *     <li>无可信列表：仅回环时优先 X-Real-IP → XFF 首 IP；否则直接返回 remoteAddr；</li>
     *     <li>remoteAddr 可信：XFF 右→左首个不可信 IP → X-Real-IP → remoteAddr；</li>
     *     <li>remoteAddr 不可信（直连）：忽略全部转发头，返回 remoteAddr。</li>
     * </ol>
     */
    private static String resolveClientIp(HttpServletRequest request, List<String> trusted) {
        String remote = normalizeIp(request.getRemoteAddr());
        if (trusted.isEmpty()) {
            if (isLoopback(remote)) {
                String realIp = firstHeaderIp(request);
                if (realIp != null) {
                    return realIp;
                }
                String xffFirst = firstXffIp(request);
                if (xffFirst != null) {
                    return xffFirst;
                }
            }
            return remote;
        }
        if (remote != null && isTrusted(remote, trusted)) {
            String chainIp = firstUntrustedFromRight(request, trusted);
            if (chainIp != null) {
                return chainIp;
            }
            String realIp = firstHeaderIp(request);
            if (realIp != null) {
                return realIp;
            }
            return remote;
        }
        return remote;
    }

    /**
     * 判断 IP 是否为可信代理（精确 IPv4/IPv6 + IPv4 CIDR；非法配置项忽略）
     *
     * @param ip       待判断 IP
     * @param trusted  可信代理列表
     * @return true 表示可信
     */
    public static boolean isTrusted(String ip, List<String> trusted) {
        if (trusted == null || trusted.isEmpty()) {
            return false;
        }
        String normalized = normalizeIp(ip);
        if (normalized == null) {
            return false;
        }
        for (String entry : trusted) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String rule = entry.trim();
            int slash = rule.indexOf('/');
            if (slash >= 0) {
                if (matchesCidr(normalized, rule)) {
                    return true;
                }
            } else {
                String candidate = normalizeIp(rule);
                if (candidate != null && candidate.equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 内部实现 ====================

    /**
     * XFF 链：右→左首个不可信 IP；全可信/无有效项返回 null
     */
    private static String firstUntrustedFromRight(HttpServletRequest request, List<String> trusted) {
        String header = request.getHeader(HttpConstants.HEADER_FORWARDED_FOR);
        if (header == null) {
            return null;
        }
        String[] parts = header.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String ip = normalizeIp(parts[i]);
            if (ip == null) {
                continue;
            }
            if (!isTrusted(ip, trusted)) {
                return ip;
            }
        }
        return null;
    }

    /**
     * XFF 链：首个（最左）合法 IP
     */
    private static String firstXffIp(HttpServletRequest request) {
        String header = request.getHeader(HttpConstants.HEADER_FORWARDED_FOR);
        if (header == null) {
            return null;
        }
        String[] parts = header.split(",");
        for (String part : parts) {
            String ip = normalizeIp(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    /**
     * 单值请求头（X-Real-IP）：取首个合法 IP
     */
    private static String firstHeaderIp(HttpServletRequest request) {
        return normalizeIp(request.getHeader(HttpConstants.HEADER_REAL_IP));
    }

    /**
     * IP 归一化：trim、过滤空值/unknown/明显非法值，{@code ::1} 归一为 {@code 127.0.0.1}
     */
    private static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String text = ip.trim();
        if (text.isEmpty() || UNKNOWN.equalsIgnoreCase(text)) {
            return null;
        }
        if (HttpConstants.LOCAL_IPV6.equals(text)) {
            return HttpConstants.LOCAL_IPV4;
        }
        if (text.indexOf(':') >= 0) {
            // IPv6 仅做基础校验（精确匹配场景），不做完整 CIDR 运算
            return text.length() <= MAX_IPV6_LENGTH ? text : null;
        }
        // IPv4 必须为合法点分十进制
        return toIpv4Long(text) == null ? null : text;
    }

    private static boolean isLoopback(String ip) {
        return HttpConstants.LOCAL_IPV4.equals(ip);
    }

    /**
     * IPv4 CIDR 位运算匹配；任何非法片段返回 false（配置项被忽略）
     */
    private static boolean matchesCidr(String ip, String cidrRule) {
        int slash = cidrRule.indexOf('/');
        if (slash <= 0 || slash == cidrRule.length() - 1) {
            return false;
        }
        Long ipValue = toIpv4Long(ip);
        if (ipValue == null) {
            return false;
        }
        String netPart = cidrRule.substring(0, slash).trim();
        Long netValue = toIpv4Long(netPart);
        if (netValue == null) {
            return false;
        }
        int prefix;
        try {
            prefix = Integer.parseInt(cidrRule.substring(slash + 1).trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (prefix < 0 || prefix > 32) {
            return false;
        }
        long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        return (ipValue & mask) == (netValue & mask);
    }

    /**
     * IPv4 点分十进制转无符号 long；格式非法返回 null
     */
    private static Long toIpv4Long(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        long value = 0L;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            int segment = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return null;
                }
                segment = segment * 10 + (c - '0');
            }
            if (segment > 255) {
                return null;
            }
            value = (value << 8) | segment;
        }
        return value;
    }

    // ==================== 内网/公网判断（保留既有能力） ====================

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
}
