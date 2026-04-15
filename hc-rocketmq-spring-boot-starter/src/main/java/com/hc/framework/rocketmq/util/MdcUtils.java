package com.hc.framework.rocketmq.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC（Mapped Diagnostic Context）工具类
 *
 * <p>用于链路追踪，在日志中统一输出 traceId。</p>
 *
 * @author hc-framework
 */
public class MdcUtils {

    /**
     * MDC 中 traceId 的 key
     */
    public static final String TRACE_ID_KEY = "traceId";

    private MdcUtils() {
    }

    /**
     * 获取当前线程的 traceId
     *
     * @return traceId，不存在则返回 null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置 traceId 到当前线程的 MDC
     *
     * @param traceId 链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 生成新的 traceId 并设置到 MDC
     *
     * @return 新生成的 traceId
     */
    public static String generateAndSetTraceId() {
        String traceId = generateTraceId();
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 生成新的 traceId（不设置到 MDC）
     *
     * @return 新生成的 traceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清除当前线程的 traceId
     */
    public static void remove() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 清除当前线程的所有 MDC 信息
     */
    public static void clear() {
        MDC.clear();
    }

}
