package com.hc.framework.rocketmq.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 幂等工具类
 *
 * <p>基于 Redis 实现消息消费的幂等性控制，防止重复消费。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 检查是否已消费
 * if (idempotentUtils.isConsumed(msgId)) {
 *     return; // 已消费，直接返回
 * }
 *
 * // 标记为已消费（设置过期时间）
 * idempotentUtils.markConsumed(msgId, 1, TimeUnit.DAYS);
 * }</pre>
 *
 * @author hc-framework
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotentUtils {

    /**
     * Redis 幂等 key 前缀
     */
    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:";

    /**
     * 默认过期时间（24小时）
     */
    private static final long DEFAULT_EXPIRE_HOURS = 24;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查消息是否已消费（使用默认过期时间）
     *
     * @param msgId 消息唯一标识
     * @return true 表示已消费，false 表示未消费
     */
    public boolean isConsumed(String msgId) {
        return isConsumed(msgId, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 检查消息是否已消费
     *
     * <p>如果 Redis 未配置，则直接返回 false（不进行幂等控制）</p>
     *
     * @param msgId    消息唯一标识
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return true 表示已消费，false 表示未消费
     */
    public boolean isConsumed(String msgId, long timeout, TimeUnit timeUnit) {
        if (redisTemplate == null) {
            log.warn("Redis 未配置，跳过幂等检查");
            return false;
        }

        String key = buildKey(msgId);
        Boolean exists = redisTemplate.hasKey(key);
        if (exists) {
            log.warn("消息重复消费: msgId={}", msgId);
            return true;
        }

        // 使用 setIfAbsent 原子操作设置，防止并发问题
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", timeout, timeUnit);
        return !Boolean.TRUE.equals(success);
    }

    /**
     * 标记消息为已消费（使用默认过期时间）
     *
     * @param msgId 消息唯一标识
     */
    public void markConsumed(String msgId) {
        markConsumed(msgId, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 标记消息为已消费
     *
     * @param msgId    消息唯一标识
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public void markConsumed(String msgId, long timeout, TimeUnit timeUnit) {
        if (redisTemplate == null) {
            return;
        }
        String key = buildKey(msgId);
        redisTemplate.opsForValue().set(key, "1", timeout, timeUnit);
    }

    /**
     * 删除幂等标记
     *
     * @param msgId 消息唯一标识
     */
    public void remove(String msgId) {
        if (redisTemplate == null) {
            return;
        }
        String key = buildKey(msgId);
        redisTemplate.delete(key);
    }

    /**
     * 构建 Redis key
     *
     * @param msgId 消息唯一标识
     * @return Redis key
     */
    private String buildKey(String msgId) {
        return IDEMPOTENT_KEY_PREFIX + msgId;
    }

}
