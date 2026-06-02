package com.hc.framework.rocketmq.util;

import org.slf4j.MDC;

/**
 * MDC（Mapped Diagnostic Context）工具类
 *
 * <p>优先委托到 hc-logging 的 TraceIdUtils，保证全链路 traceId 格式统一。
 * 当 hc-logging 不在类路径时，降级为直接操作 MDC。</p>
 *
 * @author hc-framework
 */
public class MdcUtils {

    /** MDC 中 traceId 的 key */
    public static final String TRACE_ID_KEY = "traceId";

    /** hc-logging 是否可用 */
    private static final boolean TRACE_ID_UTILS_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("com.hc.framework.logging.util.TraceIdUtils");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
        TRACE_ID_UTILS_AVAILABLE = available;
    }

    private MdcUtils() {
    }

    /**
     * 获取当前线程的 traceId
     */
    public static String getTraceId() {
        if (TRACE_ID_UTILS_AVAILABLE) {
            return com.hc.framework.logging.util.TraceIdUtils.getTraceId();
        }
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置 traceId 到当前线程的 MDC
     */
    public static void setTraceId(String traceId) {
        if (traceId == null) {
            return;
        }
        if (TRACE_ID_UTILS_AVAILABLE) {
            com.hc.framework.logging.util.TraceIdUtils.setTraceId(traceId);
        } else {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 生成新的 traceId 并设置到 MDC
     */
    public static String generateAndSetTraceId() {
        String traceId = generateTraceId();
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 生成新的 traceId（不设置到 MDC）
     */
    public static String generateTraceId() {
        if (TRACE_ID_UTILS_AVAILABLE) {
            return com.hc.framework.logging.util.TraceIdUtils.generateTraceId();
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清除当前线程的 traceId
     */
    public static void remove() {
        if (TRACE_ID_UTILS_AVAILABLE) {
            com.hc.framework.logging.util.TraceIdUtils.removeTraceId();
        } else {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 清除当前线程的 traceId（不清除其他 MDC 上下文）
     */
    public static void clear() {
        remove();
    }
}
