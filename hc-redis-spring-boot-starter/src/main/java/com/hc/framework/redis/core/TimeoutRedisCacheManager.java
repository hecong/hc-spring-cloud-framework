package com.hc.framework.redis.core;


import org.dromara.hutool.core.math.NumberUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;

/**
 * 支持自定义过期时间的 RedisCacheManager 实现类
 * 在 Cacheable.cacheNames() 格式为 "key#ttl" 时，# 后面的 ttl 为过期时间。 单位为最后一个字母（支持的单位有：d 天，h 小时，m 分钟，s 秒），默认单位为 s 秒
 *
 * @author hecong
 * @since 2026/4/1 15:51
 */
public class TimeoutRedisCacheManager extends RedisCacheManager {

    private static final String SPLIT = "#";

    public TimeoutRedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        if (StrUtil.isEmpty(name)) {
            return super.createRedisCache(name, cacheConfig);
        }

        String[] names = SplitUtil.splitToArray(name, SPLIT);
        if (names.length != 2) {
            return super.createRedisCache(name, cacheConfig);
        }

        // 修复：直接取 ttl 字符串，不要 split(":")
        String ttlStr = names[1];

        if (cacheConfig != null) {
            Duration duration = parseDuration(ttlStr);
            cacheConfig = cacheConfig.entryTtl(duration);
        }
        return super.createRedisCache(names[0], cacheConfig);
    }

    /**
     * 解析过期时间 Duration
     *
     * @param ttlStr 过期时间字符串
     * @return 过期时间 Duration
     */
    private Duration parseDuration(String ttlStr) {
        String timeUnit = StrUtil.subSuf(ttlStr, -1);
        return switch (timeUnit) {
            case "d" -> Duration.ofDays(removeDurationSuffix(ttlStr));
            case "h" -> Duration.ofHours(removeDurationSuffix(ttlStr));
            case "m" -> Duration.ofMinutes(removeDurationSuffix(ttlStr));
            case "s" -> Duration.ofSeconds(removeDurationSuffix(ttlStr));
            default -> Duration.ofSeconds(Long.parseLong(ttlStr));
        };
    }

    /**
     * 移除多余的后缀，返回具体的时间
     *
     * @param ttlStr 过期时间字符串
     * @return 时间
     */
    private Long removeDurationSuffix(String ttlStr) {
        return NumberUtil.parseLong(StrUtil.sub(ttlStr, 0, ttlStr.length() - 1));
    }
}
