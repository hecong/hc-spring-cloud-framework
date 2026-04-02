package com.hc.framework.logging.aspect;

import com.hc.framework.logging.config.LoggingProperties;
import com.hc.framework.logging.util.ServletUtils;
import com.hc.framework.logging.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dromara.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * API接口日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final LoggingProperties loggingProperties;

    /**
     * 切点：所有Controller层方法
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return point.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();

        // 忽略指定路径的日志
        List<String> ignorePaths = loggingProperties.getIgnorePaths();
        if (ignorePaths.stream().anyMatch(path -> Pattern.matches(path.replace("*", ".*"), uri))) {
            return point.proceed();
        }

        // 基础信息
        String method = request.getMethod();
        String clientIp = ServletUtils.getClientIp(request);
        String traceId = TraceIdUtils.getTraceId();

        // 记录请求参数
        Object[] args = point.getArgs();
        String params = JSONUtil.toJsonStr(args);

        Instant start = Instant.now();
        // 统一请求日志格式：[级别][TraceId][类型] 内容
        log.info("[INFO][{}][API_REQUEST] method={}, uri={}, ip={}, params={}",
            traceId, method, uri, clientIp, params);

        try {
            Object result = point.proceed();
            long cost = Duration.between(start, Instant.now()).toMillis();
            String resultStr = result != null ? JSONUtil.toJsonStr(result) : "null";
            // 截断过长的响应结果
            if (resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }
            log.info("[INFO][{}][API_RESPONSE] method={}, uri={}, cost={}ms, result={}",
                traceId, method, uri, cost, resultStr);
            return result;
        } catch (Exception e) {
            long cost = Duration.between(start, Instant.now()).toMillis();
            log.error("[ERROR][{}][API_EXCEPTION] method={}, uri={}, cost={}ms, exception={}",
                traceId, method, uri, cost, e.getMessage(), e);
            throw e;
        }
    }


}