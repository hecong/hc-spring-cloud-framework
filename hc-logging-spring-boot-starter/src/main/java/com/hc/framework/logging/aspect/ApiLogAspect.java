package com.hc.framework.logging.aspect;

import com.hc.framework.common.util.IpUtils;
import com.hc.framework.logging.config.LoggingProperties;
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

import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /** 缓存已编译的脱敏正则 Pattern */
    private final Map<String, Pattern> sanitizePatternCache = new ConcurrentHashMap<>();

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

        // 忽略指定路径的日志（使用 AntPathMatcher 支持 **/? 等标准通配符）
        List<String> ignorePaths = loggingProperties.getIgnorePaths();
        if (ignorePaths.stream().anyMatch(path -> antPathMatcher.match(path, uri))) {
            return point.proceed();
        }

        // 基础信息
        String method = request.getMethod();
        String clientIp = IpUtils.getClientIp(request);
        String traceId = TraceIdUtils.getTraceId();

        // 记录请求参数（捕获序列化异常，避免 MultipartFile 等不可序列化参数导致请求失败）
        Object[] args = point.getArgs();
        String params;
        try {
            params = sanitize(JSONUtil.toJsonStr(args));
        } catch (Exception e) {
            params = "[unserializable:" + e.getClass().getSimpleName() + "]";
        }

        Instant start = Instant.now();
        // 统一请求日志格式：[级别][TraceId][类型] 内容
        log.info("[INFO][{}][API_REQUEST] method={}, uri={}, ip={}, params={}",
            traceId, method, uri, clientIp, params);

        try {
            Object result = point.proceed();
            long cost = Duration.between(start, Instant.now()).toMillis();
            String resultStr = result != null ? sanitize(JSONUtil.toJsonStr(result)) : "null";
            // 截断过长的响应结果
            if (resultStr.length() > 500) {
                resultStr = resultStr.substring(0, 500) + "...";
            }
            log.info("[INFO][{}][API_RESPONSE] method={}, uri={}, cost={}ms, result={}",
                traceId, method, uri, cost, resultStr);
            return result;
        } catch (Exception e) {
            long cost = Duration.between(start, Instant.now()).toMillis();
            log.warn("[WARN][{}][API_EXCEPTION] method={}, uri={}, cost={}ms, exception={}",
                traceId, method, uri, cost, e.getMessage());
            throw e;
        }
    }

    /**
     * 脱敏处理：将敏感参数名对应的值替换为 ***
     *
     * <p>使用缓存的预编译正则 Pattern，避免每次请求重复编译。</p>
     */
    private String sanitize(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        List<String> sensitiveNames = loggingProperties.getSensitiveParamNames();
        if (sensitiveNames == null || sensitiveNames.isEmpty()) {
            return json;
        }
        String result = json;
        for (String name : sensitiveNames) {
            Pattern pattern = sanitizePatternCache.computeIfAbsent(name,
                    k -> Pattern.compile("(?i)(\"" + Pattern.quote(k) + "\"\\s*:\\s*)\"[^\"]*\""));
            result = pattern.matcher(result).replaceAll("$1\"***\"");
        }
        return result;
    }

}