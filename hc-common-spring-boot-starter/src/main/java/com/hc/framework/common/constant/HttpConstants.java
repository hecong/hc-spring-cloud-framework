package com.hc.framework.common.constant;

/**
 * HTTP 相关常量
 *
 * <p>统一定义项目中使用的 HTTP 请求头、响应头、协议等常量，
 * 避免硬编码字符串散落在各处，便于统一维护。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * request.getHeader(HttpConstants.HEADER_TRACE_ID);
 * response.setHeader(HttpConstants.HEADER_TOKEN, token);
 * }</pre>
 *
 * @author hc-framework
 */
public interface HttpConstants {

    // ==================== 请求头 ====================

    /**
     * 链路追踪 ID 请求头
     */
    String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 认证 Token 请求头
     */
    String HEADER_TOKEN = "Authorization";

    /**
     * Token 前缀
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * 用户 ID 请求头（服务内传递）
     */
    String HEADER_USER_ID = "X-User-Id";

    /**
     * 租户 ID 请求头
     */
    String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 真实 IP 请求头（Nginx 透传）
     */
    String HEADER_REAL_IP = "X-Real-IP";

    /**
     * 代理链 IP 请求头
     */
    String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * User-Agent 请求头
     */
    String HEADER_USER_AGENT = "User-Agent";

    // ==================== 内容类型 ====================

    /**
     * JSON 内容类型
     */
    String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    /**
     * 表单内容类型
     */
    String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 文件流内容类型
     */
    String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    // ==================== HTTP 方法 ====================

    /**
     * GET 请求
     */
    String METHOD_GET = "GET";

    /**
     * POST 请求
     */
    String METHOD_POST = "POST";

    /**
     * PUT 请求
     */
    String METHOD_PUT = "PUT";

    /**
     * DELETE 请求
     */
    String METHOD_DELETE = "DELETE";

    // ==================== 本地地址 ====================

    /**
     * 本地 IPv4 地址
     */
    String LOCAL_IPV4 = "127.0.0.1";

    /**
     * 本地 IPv6 地址
     */
    String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";
}
