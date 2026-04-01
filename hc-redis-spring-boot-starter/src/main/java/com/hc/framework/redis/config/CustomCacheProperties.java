package com.hc.framework.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author hecong
 * @since 2026/4/1 15:49
 */
@ConfigurationProperties("custom.cache")
@Data
@Validated
public class CustomCacheProperties {

    /**
     * {@link #redisScanBatchSize} 默认值
     */
    private static final Integer REDIS_SCAN_BATCH_SIZE_DEFAULT = 30;

    /**
     * redis scan 一次返回数量
     */
    private Integer redisScanBatchSize = REDIS_SCAN_BATCH_SIZE_DEFAULT;
}
