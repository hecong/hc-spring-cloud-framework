package com.hc.framework.redis.util;

import com.hc.framework.redis.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * Redis 通用序号生成器（
 * 支持：每日自增 / 永久自增 /自定义补零
 *
 * <p>每日自增通过单条 Lua 脚本原子完成 INCR 与首次 EXPIRE：
 * 仅当 key 首次创建（值为 1）时设置 2 天 TTL，后续自增不刷新 TTL，
 * 避免「先 INCR 后 EXPIRE」两步执行间中断导致的残留无过期 key。</p>
 *
 * <p>失败返回值语义：底层 Redis 调用未返回结果（返回 null）时按 0 格式化返回
 * （如长度 4 返回 {@code 0000}），调用方据此按不可用处理，不会得到看似有效但可能重复的值。</p>
 *
 * @author hecong
 */
@RequiredArgsConstructor
public class RedisSequenceGenerator {

    /**
     * 每日序列 TTL：2 天（秒），跨天兜底防重复
     */
    private static final long DAY_SEQ_TTL_SECONDS = 2 * 24 * 60 * 60L;

    /**
     * 每日自增脚本：INCR 后当且仅当值为 1（key 首次创建）时 EXPIRE
     */
    private static final String DAY_SEQ_LUA = """
        local value = redis.call('INCR', KEYS[1])
        if value == 1 then
            redis.call('EXPIRE', KEYS[1], %d)
        end
        return value
        """.formatted(DAY_SEQ_TTL_SECONDS);

    private static final RedisScript<Long> DAY_SEQ_SCRIPT =
        new DefaultRedisScript<>(DAY_SEQ_LUA, Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日自增序号（每天从 1 开始，自动过期）
     *
     * @param keyPrefix  业务唯一标识（如：order, ship, refund, out）
     * @param dateStr    日期字符串（如 20260401）
     * @param seqLength  序号长度（不足前面补 0）
     * @return 纯序号：0001；底层取值失败时返回全 0（长度与 seqLength 一致）
     */
    public String nextDaySeq(String keyPrefix, String dateStr, int seqLength) {
        String redisKey = RedisKeyConstants.SEQ + keyPrefix + ":" + dateStr;
        Long seq = redisTemplate.execute(DAY_SEQ_SCRIPT, List.of(redisKey));
        return formatSeq(seq, seqLength);
    }

    /**
     * 永久自增序号（不会重置，无 TTL）
     */
    public String nextPersistentSeq(String keyPrefix, int seqLength) {
        String redisKey = RedisKeyConstants.SEQ_GLOBAL + keyPrefix;
        Long seq = redisTemplate.opsForValue().increment(redisKey, 1);
        return formatSeq(seq, seqLength);
    }

    /**
     * 序列值格式化：失败（null）时按 0 处理，保证返回明确的不可用语义
     */
    private String formatSeq(Long seq, int seqLength) {
        long value = seq == null ? 0L : seq;
        String raw = String.valueOf(value);
        if (raw.length() >= seqLength) {
            return raw;
        }
        return "0".repeat(seqLength - raw.length()) + raw;
    }
}
