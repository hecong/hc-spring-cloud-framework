package com.hc.framework.redis.core;

import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 序列化器常量
 *
 * <p>默认白名单单例序列化器（行为与无参构造一致，白名单见
 * {@link CustomGenericJackson2JsonRedisSerializer#DEFAULT_ALLOWED_PACKAGES}）。
 * 框架自动装配（RedisTemplate / Spring Cache）已改为使用按
 * {@code hc.redis.allowed-packages} 配置构造的可配置实例，不再引用本常量；
 * 此处保留供手动装配场景及兼容既有引用使用。</p>
 */
public class RedisSerializerConstants {

    /**
     * 全局单例序列化器（默认白名单）
     */
    public static final RedisSerializer<Object> REDIS_SERIALIZER = new CustomGenericJackson2JsonRedisSerializer();

    // 私有构造器，防止实例化
    private RedisSerializerConstants() {
    }
}
