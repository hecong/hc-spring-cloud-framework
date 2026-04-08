package com.hc.framework.logging.interceptor;

import com.hc.framework.common.constant.HttpConstants;
import com.hc.framework.logging.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * TraceId 拦截器：全链路TraceId透传
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    /**
     * 请求头中的TraceId key
     */
    private static final String TRACE_ID_HEADER = HttpConstants.HEADER_TRACE_ID;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取TraceId，无则生成
        String requestTraceId = request.getHeader(TRACE_ID_HEADER);
        String traceId = TraceIdUtils.initTraceId(requestTraceId);

        // 2. 响应头返回TraceId，方便排查
        response.setHeader(TRACE_ID_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理MDC，避免线程复用导致污染
        TraceIdUtils.removeTraceId();
    }
}