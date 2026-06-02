package com.hc.framework.rocketmq.util;

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
 * // 尝试原子标记（首次消费返回 true，重复消息返回 false）
 * if (!idempotentUtils.tryMarkConsumed(msgId, 24, TimeUnit.HOURS)) {
 *     return ConsumeResult.SUCCESS; // 已消费，幂等跳过
 * }
 * try {
 *     doConsume(data);
 * } catch (Exception e) {
 *     idempotentUtils.remove(msgId); // 失败时清除标记，允许重试
 *     throw e;
 * }
 * }</pre>
 *
 * @author hc-framework
 */
@Slf4j
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
     * 只读检查消息是否已消费（不写入 Redis）
     *
     * @param msgId 消息唯一标识
     * @return true 表示已消费
     */
    public boolean isConsumed(String msgId) {
        if (redisTemplate == null) {
            return false;
        }
        return redisTemplate.hasKey(buildKey(msgId));
    }

    /**
     * 原子性尝试标记消息为已消费（检查并写入，单次 Redis 往返）
     *
     * <p>使用 {@code setIfAbsent} 保证并发安全：只有第一个到达的请求能成功标记。</p>
     *
     * @param msgId    消息唯一标识
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return true=首次消费（标记成功），false=重复消费（已存在标记）
     */
    public boolean tryMarkConsumed(String msgId, long timeout, TimeUnit timeUnit) {
        if (redisTemplate == null) {
            log.warn("Redis 未配置，跳过幂等检查");
            return true; // 无 Redis 时放行
        }

        String key = buildKey(msgId);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", timeout, timeUnit);
        if (Boolean.TRUE.equals(success)) {
            return true; // 首次消费
        }
        log.warn("消息重复消费: msgId={}", msgId);
        return false; // 已存在标记
    }

    /**
     * 原子性尝试标记消息为已消费（使用默认 24 小时过期）
     */
    public boolean tryMarkConsumed(String msgId) {
        return tryMarkConsumed(msgId, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 删除幂等标记（业务失败时调用，允许消息重试）
     *
     * @param msgId 消息唯一标识
     */
    public void remove(String msgId) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(buildKey(msgId));
    }

    private String buildKey(String msgId) {
        return IDEMPOTENT_KEY_PREFIX + msgId;
    }
}
