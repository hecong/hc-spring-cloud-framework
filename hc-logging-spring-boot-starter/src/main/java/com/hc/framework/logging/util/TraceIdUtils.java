package com.hc.framework.logging.util;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TraceId 工具类（MDC + 可传递上下文双写）
 *
 * <p>自 1.1.0 起：traceId 由"仅 MDC"升级为"MDC + {@link TransmittableThreadLocal} 双写"，
 * 使异步线程池场景可通过 TTL 捕获/回放；对外 API（get/set/remove/initTraceId）保持不变。</p>
 *
 * <p>TraceId 格式：32 位小写十六进制（前 8 位秒级时间戳 + 后 24 位随机），随机源使用
 * {@link ThreadLocalRandom}（无需密码学强度，规避高 QPS 下 SecureRandom 竞争）。</p>
 */
public class TraceIdUtils {

    /**
     * TraceId MDC key
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 可传递的 TraceId 载体：随 {@code TtlRunnable}/{@code TtlCallable} 在线程间自动传递
     */
    private static final TransmittableThreadLocal<String> TRACE_ID_HOLDER = new TransmittableThreadLocal<>();

    private TraceIdUtils() {
    }

    /**
     * 生成 TraceId（32 位十六进制：8 位秒级时间戳 + 24 位随机）
     */
    public static String generateTraceId() {
        long epochSecond = System.currentTimeMillis() / 1000;
        byte[] randomBytes = new byte[12];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(32);
        appendHex(sb, epochSecond & 0xFFFFFFFFL, 8);
        for (byte b : randomBytes) {
            appendHex(sb, b & 0xFF, 2);
        }
        return sb.toString();
    }

    /**
     * 设置 TraceId：同时写入 MDC 与可传递上下文
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 TraceId（读 MDC，保持既有契约与日志格式）
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 移除 TraceId：同时清理 MDC 与可传递上下文，避免线程池复用导致串扰
     */
    public static void removeTraceId() {
        TRACE_ID_HOLDER.remove();
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 初始化 TraceId（优先沿用入站请求头携带的 traceId，原样透传不重新生成）
     */
    public static String initTraceId(String requestTraceId) {
        String traceId = requestTraceId != null && !requestTraceId.isEmpty() ? requestTraceId : generateTraceId();
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 包装异步任务：提交线程的 TraceId 经 TTL 传递到执行线程，任务边界自动回滚 MDC
     */
    public static Runnable wrap(Runnable task) {
        String traceId = getTraceId();
        Runnable ttlTask = TtlRunnable.get(task);
        if (traceId == null || traceId.isEmpty()) {
            return ttlTask;
        }
        return () -> {
            try {
                MDC.put(TRACE_ID_KEY, traceId);
                ttlTask.run();
            } finally {
                MDC.remove(TRACE_ID_KEY);
            }
        };
    }

    /**
     * 包装异步带返回任务：语义同 {@link #wrap(Runnable)}，受检异常按 Callable 自身语义传播（不吞）
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        String traceId = getTraceId();
        Callable<T> ttlTask = TtlCallable.get(task);
        if (traceId == null || traceId.isEmpty()) {
            return ttlTask;
        }
        return () -> {
            try {
                MDC.put(TRACE_ID_KEY, traceId);
                return ttlTask.call();
            } finally {
                MDC.remove(TRACE_ID_KEY);
            }
        };
    }

    /**
     * 读取可传递上下文中的 TraceId（供装饰器在提交线程捕获值，任务线程 MDC 恢复使用）
     */
    static String getHolderTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    private static void appendHex(StringBuilder sb, long value, int digits) {
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            sb.append(Character.forDigit((int) ((value >>> shift) & 0xF), 16));
        }
    }
}
