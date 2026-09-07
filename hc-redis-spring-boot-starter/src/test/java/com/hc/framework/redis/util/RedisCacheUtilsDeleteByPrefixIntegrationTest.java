package com.hc.framework.redis.util;

import com.hc.framework.redis.support.LocalRedis;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * deleteByPrefix SCAN 分批删除集成测试（真实 Redis：随机前缀 key，测后清理）
 */
class RedisCacheUtilsDeleteByPrefixIntegrationTest {

    private static final int KEY_COUNT = 2000;

    private static RedisTemplate<String, Object> template;
    private static RedisCacheUtils cacheUtils;

    /** 每个用例独立前缀，避免并行/残留相互影响 */
    private final String runPrefix = "hc:test:del:" + System.nanoTime() + ":";

    @BeforeAll
    static void initAll() {
        LocalRedis.requireAvailable();
        template = LocalRedis.newRedisTemplate();
        cacheUtils = new RedisCacheUtils(template);
    }

    @AfterEach
    void cleanUp() {
        cacheUtils.deleteByPrefix(runPrefix);
    }

    @AfterAll
    static void closeAll() {
        LocalRedis.closeRedisTemplate(template);
    }

    private void seed(String prefix, int count) {
        for (int i = 0; i < count; i++) {
            template.opsForValue().set(prefix + i, "v-" + i);
        }
    }

    private long countKeys(String prefix) {
        return template.keys(prefix + "*").size();
    }

    @Test
    @DisplayName("删除 2000 个前缀 key，剩余匹配数为 0，删除数量返回准确")
    void deleteThousandsKeysByPrefix() {
        seed(runPrefix, KEY_COUNT);
        assertEquals(KEY_COUNT, countKeys(runPrefix), "预置数据失败");

        Long deleted = cacheUtils.deleteByPrefix(runPrefix);

        assertEquals(KEY_COUNT, deleted);
        assertEquals(0, countKeys(runPrefix), "前缀匹配 key 应被全部删除");
    }

    @Test
    @DisplayName("幂等可重入：对无匹配前缀重复删除不抛异常且返回 0")
    void deleteByPrefixIsIdempotent() {
        // 前缀尚无任何 key
        assertDoesNotThrow(() -> cacheUtils.deleteByPrefix(runPrefix));
        assertEquals(0L, cacheUtils.deleteByPrefix(runPrefix));

        // 删除后再删一次仍幂等
        seed(runPrefix, 10);
        cacheUtils.deleteByPrefix(runPrefix);
        assertEquals(0, countKeys(runPrefix));
        assertEquals(0L, cacheUtils.deleteByPrefix(runPrefix));
    }

    @Test
    @DisplayName("并发容忍：遍历期间其他客户端写入不导致删除中断")
    void deleteToleratesConcurrentWrites() throws Exception {
        seed(runPrefix, 500);
        ExecutorService writer = Executors.newSingleThreadExecutor();
        Future<?> writing = writer.submit(() -> {
            try {
                for (int i = 0; i < 200; i++) {
                    template.opsForValue().set(runPrefix + "extra-" + i, "e-" + i);
                    if (i % 50 == 0) {
                        Thread.sleep(2);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Long deleted = cacheUtils.deleteByPrefix(runPrefix);
        writing.get(10, TimeUnit.SECONDS);
        writer.shutdownNow();

        assertTrue(deleted >= 500, "预置 key 应被删除，实际 " + deleted);
        // 遍历期间写入的 key 不做强一致承诺；再次删除必须幂等不抛异常
        assertDoesNotThrow(() -> cacheUtils.deleteByPrefix(runPrefix));
    }
}
