package com.hc.framework.logging.aspect;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;

/**
 * API接口日志切面
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

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
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        // 记录请求参数
        Object[] args = point.getArgs();
        String params = JSON.toJSONString(args);

        Instant start = Instant.now();
        log.info("[API请求] {} {} | IP: {} | 参数: {}", method, uri, clientIp, params);

        try {
            Object result = point.proceed();
            long cost = Duration.between(start, Instant.now()).toMillis();
            log.info("[API响应] {} {} | 耗时: {}ms | 结果: {}", method, uri, cost,
                    result != null ? JSON.toJSONString(result).substring(0, Math.min(500, JSON.toJSONString(result).length())) : "null");
            return result;
        } catch (Exception e) {
            long cost = Duration.between(start, Instant.now()).toMillis();
            log.error("[API异常] {} {} | 耗时: {}ms | 异常: {}", method, uri, cost, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
