package com.hc.framework.redis.core;

import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 序列化器常量
 */
public class RedisSerializerConstants {

    // 全局单例序列化器
    public static final RedisSerializer<Object> REDIS_SERIALIZER = new CustomGenericJackson2JsonRedisSerializer();

    // 私有构造器，防止实例化
    private RedisSerializerConstants() {
    }
}