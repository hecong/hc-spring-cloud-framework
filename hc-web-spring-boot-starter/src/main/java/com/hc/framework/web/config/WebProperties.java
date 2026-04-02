package com.hc.framework.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
