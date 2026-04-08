package com.hc.framework.redis.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 通用序号生成器（
 * 支持：每日自增 / 永久自增 /自定义补零
 *
 * @author hecong
 */
@RequiredArgsConstructor
public class RedisSequenceGenerator {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日自增序号（每天从 1 开始，自动过期）
     *
     * @param keyPrefix  业务唯一标识（如：order, ship, refund, out）
     * @param dateStr    日期字符串（如 20260401）
     * @param seqLength  序号长度（不足前面补 0）
     * @return 纯序号：0001
     */
    public String nextDaySeq(String keyPrefix, String dateStr, int seqLength) {
        String redisKey = "seq:" + keyPrefix + ":" + dateStr;
        Long seq = redisTemplate.opsForValue().increment(redisKey, 1);
        if (seq == 1) {
            // 保留 2 天，防止跨日重复
            redisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }
        return String.format("%0" + seqLength + "d", seq);
    }

    /**
     * 永久自增序号（不会重置）
     */
    public String nextPersistentSeq(String keyPrefix, int seqLength) {
        String redisKey = "seq:global:" + keyPrefix;
        Long seq = redisTemplate.opsForValue().increment(redisKey, 1);
        return String.format("%0" + seqLength + "d", seq);
    }

}