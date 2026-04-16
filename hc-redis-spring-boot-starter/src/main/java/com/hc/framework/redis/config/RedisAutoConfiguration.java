package com.hc.framework.redis.config;

import com.hc.framework.redis.core.CustomGenericJackson2JsonRedisSerializer;
import com.hc.framework.redis.core.RedisSerializerConstants;
import com.hc.framework.redis.lock.LockTemplate;
import com.hc.framework.redis.util.RedisCacheUtils;
import com.hc.framework.redis.util.RedisSequenceGenerator;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 自动配置类
 *
 * @author hecong
 * @since 2026/4/1
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.hc.framework.redis.aspect")
public class RedisAutoConfiguration {


    /**
     * RedisTemplate 配置（Key字符串，Value JSON）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key / HashKey 使用字符串序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // Value / HashValue 使用自定义 JSON 序列化
        template.setValueSerializer(RedisSerializerConstants.REDIS_SERIALIZER);
        template.setHashValueSerializer(RedisSerializerConstants.REDIS_SERIALIZER);

        // Spring 官方要求初始化
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 分布式锁工具（仅在引入 Redisson 时生效）
     */
    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedisTemplate.class)
    public LockTemplate lockTemplate(RedissonClient redissonClient) {
        return new LockTemplate(redissonClient);
    }

    /**
     * RedisCacheUtils注入
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisCacheUtils redisCacheUtils(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheUtils(redisTemplate);
    }

    /**
     * RedisSequenceGenerator注入
     */
    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisSequenceGenerator redisSequenceGenerator(RedisTemplate<String, Object> redisTemplate) {
        return new RedisSequenceGenerator(redisTemplate);
    }
}