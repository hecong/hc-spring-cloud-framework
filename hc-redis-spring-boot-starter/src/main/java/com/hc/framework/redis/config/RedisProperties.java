package com.hc.framework.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 框架配置（hc.redis.*）
 *
 * @author hecong
 * @since 2026/9/3
 */
@ConfigurationProperties("hc.redis")
@Data
public class RedisProperties {

    /**
     * 多态反序列化扩展白名单包前缀（与默认白名单取并集，仅追加不覆盖）。
     * <p>默认白名单见 {@link com.hc.framework.redis.core.CustomGenericJackson2JsonRedisSerializer#DEFAULT_ALLOWED_PACKAGES}
     * （com.hc.framework.、com.hnhegui.、java.util.、java.lang.、java.time.）。</p>
     * <p>升级到开启白名单的框架版本前，业务必须在此配置自身实体/DTO 包前缀，
     * 否则缓存中带类型元数据的对象反序列化将被拒绝并抛出异常。</p>
     */
    private List<String> allowedPackages = new ArrayList<>();
}
