package com.hc.framework.logging.interceptor;

import com.hc.framework.logging.util.TraceIdUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceIdInterceptor：入站请求沿用/生成 + 响应头回传 + MDC 无残留
 */
class TraceIdInterceptorTest {

    private static final String HEADER = "X-Trace-Id";

    @AfterEach
    void tearDown() {
        TraceIdUtils.removeTraceId();
    }

    private TraceIdInterceptor newInterceptor() {
        return new TraceIdInterceptor();
    }

    @Test
    @DisplayName("携带请求头：原样沿用并回写响应头；afterCompletion 后 MDC 无残留")
    void propagateInboundTraceIdAndClean() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TraceIdInterceptor interceptor = newInterceptor();

        assertTrue(interceptor.preHandle(request, response, null));
        assertEquals("abc123", response.getHeader(HEADER), "响应头应回传同一 traceId");

        interceptor.afterCompletion(request, response, null, null);
        assertNull(TraceIdUtils.getTraceId(), "afterCompletion 必须清理 MDC");
    }

    @Test
    @DisplayName("未携带请求头：生成 32 位新 traceId；两次请求链路独立无串扰")
    void generateWhenMissingAndNoCrossTalk() throws Exception {
        TraceIdInterceptor interceptor = newInterceptor();
        TraceIdUtils.removeTraceId();

        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        interceptor.preHandle(req1, resp1, null);
        String first = resp1.getHeader(HEADER);
        assertTrue(first != null && first.matches("[0-9a-f]{32}"), first);
        assertEquals(first, TraceIdUtils.getTraceId());
        interceptor.afterCompletion(req1, resp1, null, null);

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        interceptor.preHandle(req2, resp2, null);
        String second = resp2.getHeader(HEADER);
        assertNotEquals(first, second, "两次请求应独立生成不同 traceId");
        assertEquals(second, TraceIdUtils.getTraceId());
        interceptor.afterCompletion(req2, resp2, null, null);
        assertNull(TraceIdUtils.getTraceId());
    }
}
