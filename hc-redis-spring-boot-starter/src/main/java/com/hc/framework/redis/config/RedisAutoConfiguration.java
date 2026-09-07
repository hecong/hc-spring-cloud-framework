package com.hc.framework.redis.config;

import com.hc.framework.redis.core.CustomGenericJackson2JsonRedisSerializer;
import com.hc.framework.redis.lock.LockTemplate;
import com.hc.framework.redis.util.RedisCacheUtils;
import com.hc.framework.redis.util.RedisSequenceGenerator;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {


    /**
     * Redis 值序列化器（Key 字符串、Value JSON）
     *
     * <p>多态反序列化白名单 = 默认白名单（com.hc.framework.、com.hnhegui.、java.util.、
     * java.lang.、java.time.）与 {@code hc.redis.allowed-packages} 的并集。
     * RedisTemplate 与 Spring Cache 共用该实例，保证同一份白名单语义。</p>
     */
    @Bean
    public CustomGenericJackson2JsonRedisSerializer redisValueSerializer(RedisProperties properties) {
        return new CustomGenericJackson2JsonRedisSerializer(properties.getAllowedPackages());
    }

    /**
     * RedisTemplate 配置（Key字符串，Value JSON）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                       CustomGenericJackson2JsonRedisSerializer redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key / HashKey 使用字符串序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // Value / HashValue 使用自定义 JSON 序列化
        template.setValueSerializer(redisValueSerializer);
        template.setHashValueSerializer(redisValueSerializer);

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