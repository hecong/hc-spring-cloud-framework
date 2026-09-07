package com.hc.framework.redis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 序列失败返回语义单测（不依赖 Redis）：
 * 底层调用返回 null（取号结果缺失）时按 0 格式化，不得抛 NPE 或返回看似有效但可能重复的值。
 */
class RedisSequenceGeneratorTest {

    @Test
    @DisplayName("每日自增：脚本执行返回 null 时返回全 0（非 NPE/非重复值）")
    void nextDaySeqReturnsZeroPaddingWhenResultMissing() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn(null);

        RedisSequenceGenerator generator = new RedisSequenceGenerator(redisTemplate);

        assertEquals("0000", generator.nextDaySeq("order", "20260903", 4));
    }

    @Test
    @DisplayName("永久自增：INCR 返回 null 时返回全 0")
    void nextPersistentSeqReturnsZeroPaddingWhenResultMissing() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(null);

        RedisSequenceGenerator generator = new RedisSequenceGenerator(redisTemplate);

        assertEquals("0000", generator.nextPersistentSeq("order", 4));
    }

    @Test
    @DisplayName("正常取值时保持补零格式")
    void normalIncrementKeepsPadding() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(42L);

        RedisSequenceGenerator generator = new RedisSequenceGenerator(redisTemplate);

        assertEquals("0042", generator.nextPersistentSeq("order", 4));
        // 每日自增也应走脚本（mock 返回正常值时同语义）
        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn(7L);
        assertEquals("0007", generator.nextDaySeq("order", "20260903", 4));
    }
}
