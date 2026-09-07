package com.hc.framework.logging.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceId 格式、双写载体与异步透传/回滚语义单测
 */
class TraceIdUtilsTest {

    @AfterEach
    void tearDown() {
        TraceIdUtils.removeTraceId();
    }

    // ---------- 3.1 格式 ----------

    @Test
    @DisplayName("生成 32 位小写 hex：前 8 位为秒级时间戳 hex，无连字符")
    void generateFormat() {
        String traceId = TraceIdUtils.generateTraceId();
        assertTrue(traceId.matches("[0-9a-f]{32}"), "应为 32 位小写 hex: " + traceId);
        long now = System.currentTimeMillis() / 1000;
        long prefix = Long.parseLong(traceId.substring(0, 8), 16);
        assertTrue(Math.abs(prefix - now) <= 3, "前 8 位应为秒级时间戳: " + traceId);
    }

    @Test
    @DisplayName("并发大批量生成无重复（24 位随机后缀）")
    void generateUniqueUnderConcurrency() throws Exception {
        int threadCount = 8;
        int perThread = 1000;
        Set<String> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            AtomicInteger expected = new AtomicInteger();
            for (int t = 0; t < threadCount; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        ids.add(TraceIdUtils.generateTraceId());
                        expected.incrementAndGet();
                    }
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
            assertEquals(expected.get(), ids.size(), "应无重复生成");
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------- 3.2 入站原样沿用 + 双写一致性 ----------

    @Test
    @DisplayName("请求头 traceId（含旧 16 位格式）原样沿用不被改写")
    void initTraceIdKeepsInboundValue() {
        String legacy = "0123456789abcdef";
        String returned = TraceIdUtils.initTraceId(legacy);
        assertEquals(legacy, returned, "返回值为沿用值");
        assertEquals(legacy, TraceIdUtils.getTraceId(), "MDC 与返回一致");
    }

    @Test
    @DisplayName("set/remove 双写同步清理：无 MDC 残留，可传递上下文同步清空")
    void doubleWriteAndClean() {
        TraceIdUtils.setTraceId("abc");
        assertEquals("abc", TraceIdUtils.getTraceId());
        TraceIdUtils.removeTraceId();
        assertNull(TraceIdUtils.getTraceId());
    }

    // ---------- 1.3 异步透传 / 回滚 / 异常不吞 ----------

    @Test
    @DisplayName("wrap(Runnable)：任务线程 MDC 含提交线程 traceId，任务结束后回滚无残留")
    void wrapRunnablePropagatesAndRollsBack() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            TraceIdUtils.setTraceId("task-runnable-1");
            CompletableFuture<String> inside = new CompletableFuture<>();
            pool.submit(TraceIdUtils.wrap(() -> inside.complete(TraceIdUtils.getTraceId())));
            assertEquals("task-runnable-1", inside.get(5, TimeUnit.SECONDS), "任务执行期间应继承提交线程 traceId");

            // 同一线程复用执行无 traceId 的普通任务：不得残留上一任务 traceId
            CompletableFuture<String> reuse = new CompletableFuture<>();
            pool.submit(() -> reuse.complete(TraceIdUtils.getTraceId()));
            assertNull(reuse.get(5, TimeUnit.SECONDS), "线程复用不得残留 traceId");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("wrap(Callable)：返回值与异常语义保持，受检异常不被吞；MDC 回滚")
    void wrapCallableKeepsSemanticsAndRollsBack() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            TraceIdUtils.setTraceId("task-callable-1");
            CompletableFuture<String> result = new CompletableFuture<>();
            pool.submit(TraceIdUtils.wrap(() -> {
                result.complete(TraceIdUtils.getTraceId());
                return "done";
            })).get(5, TimeUnit.SECONDS);
            assertEquals("task-callable-1", result.get(5, TimeUnit.SECONDS));

            // 异常传播且不被吞
            ExecutionException thrown = assertThrows(ExecutionException.class, () -> pool.submit(
                    TraceIdUtils.wrap((java.util.concurrent.Callable<Object>) () -> {
                        throw new IllegalStateException("boom");
                    })).get(5, TimeUnit.SECONDS));
            assertTrue(thrown.getCause() instanceof IllegalStateException, "异常应保持原语义传播");

            // 回滚仍成立
            CompletableFuture<String> after = new CompletableFuture<>();
            pool.submit(() -> after.complete(TraceIdUtils.getTraceId()));
            assertNull(after.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("提交线程无 traceId 时：任务不写入也不残留 MDC")
    void wrapWithoutContextStaysClean() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture<String> inside = new CompletableFuture<>();
            pool.submit(TraceIdUtils.wrap(() -> inside.complete(String.valueOf(TraceIdUtils.getTraceId()))));
            assertEquals("null", inside.get(5, TimeUnit.SECONDS), "无上下文不得伪造 traceId");

            CompletableFuture<String> reuse = new CompletableFuture<>();
            pool.submit(() -> reuse.complete(TraceIdUtils.getTraceId()));
            assertNull(reuse.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("wrap(Runnable) 包装异常不被吞，且任务结束后 MDC 回滚")
    void wrapRunnableExceptionObservable() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            TraceIdUtils.setTraceId("task-err");
            java.util.concurrent.Future<?> future = pool.submit(TraceIdUtils.wrap((Runnable) () -> {
                throw new IllegalArgumentException("runnable boom");
            }));
            ExecutionException e = assertThrows(ExecutionException.class,
                    () -> future.get(5, TimeUnit.SECONDS));
            assertTrue(e.getCause() instanceof IllegalArgumentException);

            CompletableFuture<String> reuse = new CompletableFuture<>();
            pool.submit(() -> reuse.complete(TraceIdUtils.getTraceId()));
            assertNull(reuse.get(5, TimeUnit.SECONDS), "异常路径 MDC 同样回滚");
        } finally {
            pool.shutdownNow();
        }
    }
}
