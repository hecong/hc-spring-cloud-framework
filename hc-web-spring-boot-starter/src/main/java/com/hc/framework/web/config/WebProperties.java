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
    private boolean xssEnabled = true;

    /**
     * XSS 放行路径（富文本/编辑器等）
     */
    private List<String> xssExcludeUrls = new ArrayList<>();
}
