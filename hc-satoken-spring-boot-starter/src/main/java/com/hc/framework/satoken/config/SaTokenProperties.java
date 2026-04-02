package com.hc.framework.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sa-Token配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.satoken")
public class SaTokenProperties {

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * token名称
     */
    private String tokenName = "satoken";

    /**
     * token有效期（单位：秒）默认30天
     */
    private Long timeout = 2592000L;

    /**
     * token临时有效期（指定时间内无操作就视为token过期）单位：秒
     */
    private Long activityTimeout = -1L;

    /**
     * 是否允许同一账号并发登录
     */
    private Boolean isConcurrent = true;

    /**
     * 是否共享token
     */
    private Boolean isShare = true;

    /**
     * token风格
     */
    private String tokenStyle = "uuid";

    /**
     * 是否输出操作日志
     */
    private Boolean isLog = false;

    /**
     * 是否从cookie中读取token
     */
    private Boolean isReadCookie = true;

    /**
     * 是否从请求体中读取token
     */
    private Boolean isReadBody = true;

    /**
     * 是否从header中读取token
     */
    private Boolean isReadHeader = true;
}
