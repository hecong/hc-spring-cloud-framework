package com.hc.framework.logging.util;

import org.slf4j.MDC;

import java.util.UUID;


/**
 * TraceId 工具类（基于MDC）
 */
public class TraceIdUtils {

    /**
     * TraceId MDC key
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 生成TraceId（UUID简化版）
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置TraceId到MDC
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 移除MDC中的TraceId
     */
    public static void removeTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }

    /**
     * 初始化TraceId（优先从请求头获取，无则生成）
     */
    public static String initTraceId(String requestTraceId) {
        String traceId = requestTraceId != null && !requestTraceId.isEmpty() ? requestTraceId : generateTraceId();
        setTraceId(traceId);
        return traceId;
    }
}