package com.hc.framework.redis.lock;

import com.hc.framework.redis.support.LocalRedis;
import com.hc.framework.web.exception.BusinessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LockTemplate 异常语义集成测试（真实 Redis + Redisson）：
 * - 业务/运行时异常原样透传
 * - 受检异常包装 LOCK_EXECUTION_FAILED（cause 保留）
 * - 中断恢复中断位并抛 LOCK_INTERRUPTED
 * - 全部路径锁均释放，可再次获取
 */
class LockTemplateIntegrationTest {

    private static RedissonClient redisson;
    private static LockTemplate lockTemplate;

    @BeforeAll
    static void initAll() {
        LocalRedis.requireAvailable();
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + LocalRedis.HOST + ":" + LocalRedis.PORT);
        redisson = Redisson.create(config);
        lockTemplate = new LockTemplate(redisson);
    }

    @AfterAll
    static void closeAll() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    private String newLockKey() {
        return "lock:it:" + UUID.randomUUID();
    }

    @Test
    @DisplayName("业务异常原样透传：捕获类型/错误码/消息不变，锁已释放且可再次获取")
    void businessExceptionPassesThroughAndLockReleased() {
        String key = newLockKey();
        BusinessException biz = new BusinessException(2001, "库存不足");

        BusinessException thrown = assertThrows(BusinessException.class,
            () -> lockTemplate.execute(key, () -> {
                throw biz;
            }));

        assertSame(biz, thrown, "必须是原异常实例（未被包装）");
        assertEquals(2001, thrown.getCode());
        assertEquals("库存不足", thrown.getMessage());

        assertFalse(lockTemplate.isLocked(key), "异常路径后锁必须已释放");
        assertEquals("ok", lockTemplate.execute(key, () -> "ok"), "后续线程应可再次获取同一把锁");
    }

    @Test
    @DisplayName("IllegalArgumentException 等运行时异常原样透传且锁释放")
    void illegalArgumentExceptionPassesThroughAndLockReleased() {
        String key = newLockKey();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> lockTemplate.execute(key, () -> {
                throw new IllegalArgumentException("参数不合法");
            }));

        assertEquals("参数不合法", thrown.getMessage());
        assertFalse(lockTemplate.isLocked(key));
    }

    @Test
    @DisplayName("回调抛受检异常：包装为 LOCK_EXECUTION_FAILED 且 cause 保留、锁释放")
    void checkedExceptionWrappedAsExecutionFailed() {
        String key = newLockKey();

        LockException thrown = assertThrows(LockException.class,
            () -> lockTemplate.execute(key, () -> {
                throw new IOException("redis 之外的底层 IO 失败");
            }));

        assertEquals(LockException.LOCK_EXECUTION_FAILED, thrown.getCode());
        assertInstanceOf(IOException.class, thrown.getCause(), "原始异常应作为 cause 保留");
        assertFalse(lockTemplate.isLocked(key));
        assertEquals("ok", lockTemplate.execute(key, () -> "ok"));
    }

    @Test
    @DisplayName("回调抛 InterruptedException：恢复中断标志并抛 LOCK_INTERRUPTED、锁释放")
    void interruptedDuringCallbackRestoresFlagAndReportsInterrupted() {
        String key = newLockKey();
        try {
            LockException thrown = assertThrows(LockException.class,
                () -> lockTemplate.execute(key, () -> {
                    throw new InterruptedException("执行被中断");
                }));

            assertEquals(LockException.LOCK_INTERRUPTED, thrown.getCode());
            assertTrue(Thread.currentThread().isInterrupted(), "中断标志必须被恢复");
        } finally {
            // 清理中断标志，避免影响同线程后续用例
            Thread.interrupted();
        }
        assertFalse(lockTemplate.isLocked(key), "中断路径后锁必须已释放");
        assertEquals("ok", lockTemplate.execute(key, () -> "ok"));
    }
}
