package com.hc.framework.logging.aspect;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.hc.framework.logging.annotation.RateLimiter;
import com.hc.framework.web.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 接口限流切面
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    /**
     * 切点：带有@RateLimiter注解的方法
     */
    @Pointcut("@annotation(com.hc.framework.logging.annotation.RateLimiter)")
    public void rateLimiterPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("rateLimiterPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);

        String resourceName = getResourceName(method, rateLimiter);

        // 初始化限流规则
        initFlowRule(resourceName, rateLimiter);

        Entry entry = null;
        try {
            entry = SphU.entry(resourceName);
            return point.proceed();
        } catch (BlockException e) {
            log.warn("接口限流触发: {}", resourceName);
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 获取资源名称
     */
    private String getResourceName(Method method, RateLimiter rateLimiter) {
        String baseName = method.getDeclaringClass().getName() + ":" + method.getName();

        switch (rateLimiter.dimension()) {
            case IP:
                return baseName + ":" + getClientIp();
            case USER:
                return baseName + ":" + getCurrentUserId();
            case GLOBAL:
            default:
                return baseName;
        }
    }

    /**
     * 初始化限流规则
     */
    private void initFlowRule(String resourceName, RateLimiter rateLimiter) {
        List<FlowRule> rules = FlowRuleManager.getRules();
        boolean exists = rules.stream().anyMatch(r -> r.getResource().equals(resourceName));

        if (!exists) {
            FlowRule rule = new FlowRule();
            rule.setResource(resourceName);
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(rateLimiter.qps());

            switch (rateLimiter.mode()) {
                case WARM_UP:
                    rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
                    rule.setWarmUpPeriodSec(10);
                    break;
                case RATE_LIMITER:
                    rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
                    rule.setMaxQueueingTimeMs(500);
                    break;
                default:
                    rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
            }

            List<FlowRule> newRules = new ArrayList<>(rules);
            newRules.add(rule);
            FlowRuleManager.loadRules(newRules);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        // TODO: 从Sa-Token或其他认证框架获取当前用户ID
        return "anonymous";
    }
}
