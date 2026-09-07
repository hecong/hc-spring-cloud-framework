package com.hc.framework.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Web配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.web")
public class WebProperties {

    /**
     * 是否启用全局异常处理
     */
    private Boolean enabled = true;

    /**
     * 是否包装响应结果
     */
    private Boolean wrapResponse = true;

    /**
     * 响应码字段名
     */
    private String codeField = "code";

    /**
     * 响应消息字段名
     */
    private String messageField = "message";

    /**
     * 响应数据字段名
     */
    private String dataField = "data";

    /**
     * 是否开启XSS防护（默认开启）
     */
    private Boolean xssEnabled = true;

    /**
     * XSS 放行路径（富文本/编辑器等）
     */
    private List<String> xssExcludeUrls = new ArrayList<>();

    /**
     * 请求体缓存大小上限（字节），默认 2MB；超过上限的请求体放弃整体缓存，直接消费原始流
     */
    private long maxCachedBodySize = 2L * 1024 * 1024;

    /**
     * 可信代理 IP / CIDR 列表，默认空 = 不信任任何代理头，客户端 IP 直接取 TCP 对端地址；
     * 配置后从 X-Forwarded-For 右向左跳过可信代理取首个不可信 IP
     */
    private List<String> trustedProxies = new ArrayList<>();
}
