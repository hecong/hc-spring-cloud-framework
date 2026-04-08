package com.hc.framework.redis.config;

import com.hc.framework.redis.core.CustomGenericJackson2JsonRedisSerializer;
import com.hc.framework.redis.core.TimeoutRedisCacheManager;
import org.dromara.hutool.core.text.StrUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

import java.util.Objects;

@AutoConfiguration
@EnableConfigurationProperties({CacheProperties.class, CustomCacheProperties.class})
@EnableCaching
public class CacheAutoConfiguration {

    // 全局单例序列化器
    private static final RedisSerializer<Object> REDIS_SERIALIZER = new CustomGenericJackson2JsonRedisSerializer();

    @Bean
    @Primary
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        // 自定义 Key 前缀
        config = config.computePrefixWith(cacheName -> {
            String keyPrefix = cacheProperties.getRedis().getKeyPrefix();
            if (StringUtils.hasText(keyPrefix)) {
                keyPrefix = keyPrefix.lastIndexOf(StrUtil.COLON) == -1 ? keyPrefix + StrUtil.COLON : keyPrefix;
                return keyPrefix + cacheName + StrUtil.COLON;
            }
            return cacheName + StrUtil.COLON;
        });

        // 使用自定义 JSON 序列化
        config = config.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(REDIS_SERIALIZER));

        // 加载配置文件
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisTemplate<String, Object> redisTemplate,
                                               RedisCacheConfiguration redisCacheConfiguration,
                                               CustomCacheProperties customCacheProperties) {
        RedisConnectionFactory connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory());

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
            connectionFactory,
            BatchStrategies.scan(customCacheProperties.getRedisScanBatchSize())
        );

        return new TimeoutRedisCacheManager(cacheWriter, redisCacheConfiguration);
    }
}