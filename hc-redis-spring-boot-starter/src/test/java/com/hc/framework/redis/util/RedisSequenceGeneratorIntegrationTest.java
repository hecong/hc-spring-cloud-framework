package com.hc.framework.redis.util;

import com.hc.framework.redis.constant.RedisKeyConstants;
import com.hc.framework.redis.support.LocalRedis;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 序列原子性集成测试（真实 Redis）：
 * - 首次取号设置 TTL、已有 key 自增不刷新 TTL（原子 Lua）
 * - 100 线程并发取号互不重复
 */
class RedisSequenceGeneratorIntegrationTest {

    private static final String DATE_STR = "20260903";
    private static final int CONCURRENCY = 100;

    private static RedisTemplate<String, Object> template;
    private static RedisSequenceGenerator generator;

    private final String bizPrefix = "it-" + System.nanoTime();

    @BeforeAll
    static void initAll() {
        LocalRedis.requireAvailable();
        template = LocalRedis.newRedisTemplate();
        generator = new RedisSequenceGenerator(template);
    }

    @AfterEach
    void cleanUp() {
        template.delete(RedisKeyConstants.SEQ + bizPrefix + ":" + DATE_STR);
        template.delete(RedisKeyConstants.SEQ_GLOBAL + bizPrefix);
    }

    @AfterAll
    static void closeAll() {
        LocalRedis.closeRedisTemplate(template);
    }

    @Test
    @DisplayName("首次取号返回 1 且带 2 天 TTL；再次取号不刷新 TTL")
    void firstCallSetsTtlAndFollowUpDoesNotRefresh() throws InterruptedException {
        String redisKey = RedisKeyConstants.SEQ + bizPrefix + ":" + DATE_STR;

        String first = generator.nextDaySeq(bizPrefix, DATE_STR, 4);
        assertEquals("0001", first);

        Long expireSecondsAfterFirst = template.getExpire(redisKey, TimeUnit.SECONDS);
        assertNotNull(expireSecondsAfterFirst);
        assertTrue(expireSecondsAfterFirst > 0 && expireSecondsAfterFirst <= 2 * 24 * 60 * 60,
            "首次取号后 key 应带 2 天 TTL，实际 " + expireSecondsAfterFirst);

        // 等待足够时间使 TTL 秒级读数可区分（Redis EXPIRE 精度为秒）
        Thread.sleep(2100);

        String second = generator.nextDaySeq(bizPrefix, DATE_STR, 4);
        assertEquals("0002", second);

        Long expireSecondsAfterSecond = template.getExpire(redisKey, TimeUnit.SECONDS);
        assertNotNull(expireSecondsAfterSecond);
        assertTrue(expireSecondsAfterSecond > 0, "已有 key 自增后仍应带 TTL");
        assertTrue(expireSecondsAfterSecond < expireSecondsAfterFirst,
            "已有 key 自增不应刷新 TTL（剩余 " + expireSecondsAfterSecond + " 应小于首次 " + expireSecondsAfterFirst + "）");
    }

    @Test
    @DisplayName("100 线程并发取同一业务 key 序列号，100 个值互不重复")
    void concurrentSequencesAreUnique() throws Exception {
        String date = DATE_STR;
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>(CONCURRENCY);
            for (int i = 0; i < CONCURRENCY; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return generator.nextDaySeq(bizPrefix, date, 5);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "并发任务应全部就绪");
            start.countDown();

            List<String> values = new ArrayList<>(CONCURRENCY);
            for (Future<String> future : futures) {
                values.add(future.get(15, TimeUnit.SECONDS));
            }

            assertEquals(CONCURRENCY, values.size());
            assertEquals(CONCURRENCY, values.stream().distinct().count(), "并发取号不得重复: " + values);
            assertTrue(values.stream().allMatch(v -> v != null && v.matches("\\d{5}")));
        } finally {
            pool.shutdownNow();
        }
    }
}
