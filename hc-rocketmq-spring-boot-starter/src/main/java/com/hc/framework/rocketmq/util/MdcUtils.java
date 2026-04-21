package com.hc.framework.rocketmq.util;

import com.hc.framework.logging.util.TraceIdUtils;
import org.slf4j.MDC;

/**
 * MDC（Mapped Diagnostic Context）工具类
 *
 * <p>委托到 {@link TraceIdUtils}，保持全链路 traceId 格式统一。</p>
 *
 * @author hc-framework
 */
public class MdcUtils {

    /**
     * MDC 中 traceId 的 key
     */
    public static final String TRACE_ID_KEY = TraceIdUtils.TRACE_ID_KEY;

    private MdcUtils() {
    }

    /**
     * 获取当前线程的 traceId
     *
     * @return traceId，不存在则返回 null
     */
    public static String getTraceId() {
        return TraceIdUtils.getTraceId();
    }

    /**
     * 设置 traceId 到当前线程的 MDC
     *
     * @param traceId 链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        TraceIdUtils.setTraceId(traceId);
    }

    /**
     * 生成新的 traceId 并设置到 MDC
     *
     * @return 新生成的 traceId
     */
    public static String generateAndSetTraceId() {
        String traceId = TraceIdUtils.generateTraceId();
        TraceIdUtils.setTraceId(traceId);
        return traceId;
    }

    /**
     * 生成新的 traceId（不设置到 MDC）
     *
     * @return 新生成的 traceId
     */
    public static String generateTraceId() {
        return TraceIdUtils.generateTraceId();
    }

    /**
     * 清除当前线程的 traceId
     */
    public static void remove() {
        TraceIdUtils.removeTraceId();
    }

    /**
     * 清除当前线程的所有 MDC 信息
     */
    public static void clear() {
        MDC.clear();
    }

}
