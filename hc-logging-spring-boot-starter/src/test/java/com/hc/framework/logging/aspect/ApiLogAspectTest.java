package com.hc.framework.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hc.framework.logging.config.LoggingProperties;
import com.hc.framework.logging.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * ApiLogAspect 采样（3.3）、限长截断（3.4）、脱敏与异常不受采样控制单测
 */
class ApiLogAspectTest {

    private LoggingProperties props;
    private ApiLogAspect aspect;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        props = new LoggingProperties();
        props.setSensitiveParamNames(List.of("password", "idCard"));
        props.setIgnorePaths(List.of());
        aspect = new ApiLogAspect(props);

        Logger logger = (Logger) LoggerFactory.getLogger(ApiLogAspect.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiLogAspect.class);
        logger.detachAppender(appender);
        appender.stop();
        TraceIdUtils.removeTraceId();
        RequestContextHolder.resetRequestAttributes();
    }

    private void useContext(String traceId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        if (traceId != null) {
            TraceIdUtils.setTraceId(traceId);
        } else {
            TraceIdUtils.removeTraceId();
        }
    }

    private ProceedingJoinPoint pointStub(boolean throwEx, Object result) throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        doReturn(new Object[]{Map.of("username", "hc", "password", "secret123")}).when(point).getArgs();
        if (throwEx) {
            doThrow(new RuntimeException("boom")).when(point).proceed();
        } else {
            doReturn(result).when(point).proceed();
        }
        return point;
    }

    private List<String> invokeOnce(String traceId, boolean throwEx, Object result) throws Throwable {
        useContext(traceId);
        return invokeCurrent(throwEx, result);
    }

    private List<String> invokeCurrent(boolean throwEx, Object result) throws Throwable {
        int before = appender.list.size();
        ProceedingJoinPoint point = pointStub(throwEx, result);
        if (throwEx) {
            assertThrows(RuntimeException.class, () -> aspect.around(point));
        } else {
            aspect.around(point);
        }
        return appender.list.subList(before, appender.list.size()).stream()
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    private boolean logged(List<String> messages, String marker) {
        return messages.stream().anyMatch(m -> m.contains(marker));
    }

    @Test
    @DisplayName("默认全量采样：请求/响应日志落盘且敏感参数被脱敏")
    void defaultFullSamplingLogsAndSanitizes() throws Throwable {
        List<String> messages = invokeOnce("trace-full-1", false,
                Map.of("code", 200, "data", Map.of("idCard", "110101199001011234", "name", "hc")));

        assertTrue(logged(messages, "API_REQUEST"), messages.toString());
        assertTrue(logged(messages, "API_RESPONSE"), messages.toString());
        String responseLine = messages.stream().filter(m -> m.contains("API_RESPONSE")).findFirst().orElse("");
        assertTrue(responseLine.contains("\"idCard\":\"***\""), "响应敏感字段应脱敏: " + responseLine);
        String requestLine = messages.stream().filter(m -> m.contains("API_REQUEST")).findFirst().orElse("");
        assertTrue(requestLine.contains("\"password\":\"***\""), "请求敏感字段应脱敏: " + requestLine);
        assertFalse(requestLine.contains("secret123"));
    }

    @Test
    @DisplayName("sampleRate=0.5：同一 traceId 判定一致（不抖动），采样/未采样均稳定")
    void samplingDeterministicPerTraceId() throws Throwable {
        props.getApiLog().setSampleRate(0.5);

        // 找一个"被采样"与一个"未采样"的 traceId
        String sampledId = null;
        String unsampledId = null;
        for (int i = 0; i < 500 && (sampledId == null || unsampledId == null); i++) {
            String candidate = "trace-" + i;
            List<String> first = invokeOnce(candidate, false, Map.of("code", 200));
            if (sampledId == null && logged(first, "API_RESPONSE")) {
                sampledId = candidate;
            } else if (unsampledId == null && !logged(first, "API_RESPONSE")) {
                unsampledId = candidate;
            }
        }
        assertTrue(sampledId != null && unsampledId != null, "应能同时找到采样/未采样样本");

        // 同一 traceId 重复调用，判定不变
        List<String> repeat1 = invokeOnce(sampledId, false, Map.of("code", 200));
        List<String> repeat2 = invokeOnce(sampledId, false, Map.of("code", 200));
        assertTrue(logged(repeat1, "API_RESPONSE") && logged(repeat2, "API_RESPONSE"),
                "同一 traceId 采样结果必须一致");
        List<String> miss1 = invokeOnce(unsampledId, false, Map.of("code", 200));
        List<String> miss2 = invokeOnce(unsampledId, false, Map.of("code", 200));
        assertFalse(logged(miss1, "API_RESPONSE") || logged(miss2, "API_RESPONSE"),
                "同一 traceId 未采样结果必须一致");
    }

    @Test
    @DisplayName("未采样请求抛异常：仍记录 API_EXCEPTION（异常不受采样控制）")
    void unsampledExceptionStillLogged() throws Throwable {
        props.getApiLog().setSampleRate(0.0);
        List<String> messages = invokeOnce("trace-ex-1", true, null);
        assertTrue(logged(messages, "API_EXCEPTION"), messages.toString());
        assertFalse(logged(messages, "API_REQUEST"), "未采样不记录请求日志");
    }

    @Test
    @DisplayName("超 maxSerializeLength：响应仅记录截断标记，不输出超限完整内容")
    void responseOverLimitTruncated() throws Throwable {
        props.getApiLog().setMaxSerializeLength(100);
        String hugeResult = "data".repeat(50_000);
        List<String> messages = invokeOnce("trace-over-1", false, hugeResult);
        String responseLine = messages.stream().filter(m -> m.contains("API_RESPONSE")).findFirst().orElse("");
        assertTrue(responseLine.contains("<truncated, len=100>"), responseLine);
        assertFalse(responseLine.contains("datadata"), "不得输出超限完整内容");
    }

    @Test
    @DisplayName("阈值内响应完整输出；null 响应记录为 null")
    void underLimitResponseFullAndNullSupported() throws Throwable {
        List<String> messages = invokeOnce("trace-under-1", false, null);
        assertTrue(logged(messages, "result=null"), messages.toString());
        List<String> normal = invokeOnce("trace-under-2", false, "hello");
        assertTrue(normal.stream().anyMatch(m -> m.contains("API_RESPONSE") && m.contains("result=\"hello\"")),
                normal.toString());
    }

    @Test
    @DisplayName("ignorePaths 命中：完全不记录任何 API 日志")
    void ignoredPathSkipsLogging() throws Throwable {
        props.setIgnorePaths(List.of("/actuator/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TraceIdUtils.setTraceId("trace-ign-1");

        ProceedingJoinPoint point = pointStub(false, "ok");
        Object result = aspect.around(point);

        assertTrue(appender.list.isEmpty(), "忽略路径不得记录日志");
        assertEquals("ok", result);
    }
}
