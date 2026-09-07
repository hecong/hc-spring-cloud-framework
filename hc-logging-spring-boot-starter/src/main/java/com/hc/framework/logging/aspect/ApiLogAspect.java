package com.hc.framework.logging.aspect;

import com.hc.framework.common.util.IpUtils;
import com.hc.framework.logging.config.LoggingProperties;
import com.hc.framework.logging.util.LimitedJsonSerializer;
import com.hc.framework.logging.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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
 * API接口日志切面：确定性采样 + 限长序列化 + 脱敏
 *
 * <p>自 1.1.0 起：</p>
 * <ul>
 *     <li>采样：{@code hc.logging.apiLog.sampleRate}（默认 1.0 全量），按 traceId 取模，同一链路判定一致；
 *         异常日志不受采样控制（未采样请求抛异常仍记录）。</li>
 *     <li>体积：序列化超 {@code hc.logging.apiLog.maxSerializeLength}（默认 4096）只记录
 *         {@code <truncated, len=N>}，避免全量物化后硬编码截断。</li>
 * </ul>
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

        // 采样判定（一次判定，请求/响应共用；异常路径不受采样控制）
        boolean sampled = shouldSample(traceId);
        int maxSerializeLength = loggingProperties.getApiLog().getMaxSerializeLength();

        Instant start = Instant.now();
        if (sampled) {
            // 记录请求参数（捕获序列化异常，避免 MultipartFile 等不可序列化参数导致请求失败）
            Object[] args = point.getArgs();
            String params;
            try {
                params = sanitize(LimitedJsonSerializer.toLimitedJson(args, maxSerializeLength));
            } catch (Exception e) {
                params = "[unserializable:" + e.getClass().getSimpleName() + "]";
            }
            // 统一请求日志格式：[级别][TraceId][类型] 内容
            log.info("[INFO][{}][API_REQUEST] method={}, uri={}, ip={}, params={}",
                    traceId, method, uri, clientIp, params);
        }

        try {
            Object result = point.proceed();
            long cost = Duration.between(start, Instant.now()).toMillis();
            if (!sampled) {
                return result;
            }
            String resultStr;
            if (result == null) {
                resultStr = "null";
            } else {
                try {
                    resultStr = sanitize(LimitedJsonSerializer.toLimitedJson(result, maxSerializeLength));
                } catch (Exception e) {
                    resultStr = "[unserializable:" + e.getClass().getSimpleName() + "]";
                }
            }
            log.info("[INFO][{}][API_RESPONSE] method={}, uri={}, cost={}ms, result={}",
                    traceId, method, uri, cost, resultStr);
            return result;
        } catch (Exception e) {
            long cost = Duration.between(start, Instant.now()).toMillis();
            // 异常日志不受采样控制：未采样请求抛异常也记录，保证故障可观测
            log.warn("[WARN][{}][API_EXCEPTION] method={}, uri={}, cost={}ms, exception={}",
                    traceId, method, uri, cost, e.getMessage());
            throw e;
        }
    }

    /**
     * 采样判定：sampleRate>=1 全量；0.5 表示按 traceId 哈希约一半请求记录（同一 traceId 恒定一致）
     */
    private boolean shouldSample(String traceId) {
        Double sampleRate = loggingProperties.getApiLog().getSampleRate();
        double rate = sampleRate == null ? 1.0 : sampleRate;
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        // 无 traceId（非 HTTP 上下文等）按全量兜底
        if (traceId == null || traceId.isEmpty()) {
            return true;
        }
        long bucket = (traceId.hashCode() & 0xFFFFFFFFL) % 1000L;
        return bucket < Math.round(rate * 1000);
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
