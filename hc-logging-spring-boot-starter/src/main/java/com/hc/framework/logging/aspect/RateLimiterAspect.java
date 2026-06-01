package com.hc.framework.logging.aspect;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.hc.framework.common.util.IpUtils;
import com.hc.framework.logging.annotation.RateLimiter;
import com.hc.framework.logging.config.LoggingProperties;
import com.hc.framework.logging.spi.UserIdResolver;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 接口限流切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final LoggingProperties loggingProperties;

    private final UserIdResolver userIdResolver;

    /**
     * 记录本切面注册的 Sentinel 资源名，用于在销毁时清理
     */
    private final Set<String> registeredResources = ConcurrentHashMap.newKeySet();

    /**
     * 切点：标注@RateLimiter的方法/类
     */
    @org.aspectj.lang.annotation.Pointcut("@annotation(com.hc.framework.logging.annotation.RateLimiter) || @within(com.hc.framework.logging.annotation.RateLimiter)")
    public void rateLimiterPointcut() {
    }

    /**
     * 环绕通知实现限流
     */
    @Around("rateLimiterPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 1. 获取限流注解配置
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimiter annotation = method.getAnnotation(RateLimiter.class);
        if (annotation == null) {
            annotation = point.getTarget().getClass().getAnnotation(RateLimiter.class);
        }
        // 默认配置兜底
        double qps = annotation != null ? annotation.qps() : loggingProperties.getRateLimit().getDefaultQps();
        RateLimiter.Mode mode = annotation != null ? annotation.mode() : RateLimiter.Mode.DEFAULT;
        RateLimiter.Dimension dimension = annotation != null ? annotation.dimension() : RateLimiter.Dimension.GLOBAL;

        // 2. 构建限流资源名（按维度区分）
        String resourceName = buildResourceName(point, dimension);

        // 3. 初始化限流规则
        initFlowRule(resourceName, qps, mode);

        // 4. 执行限流判断
        Entry entry = null;
        try {
            entry = SphU.entry(resourceName, EntryType.IN);
            return point.proceed();
        } catch (BlockException e) {
            log.error("[接口限流] 资源: {} | QPS限制: {} | 限流维度: {} | 异常: {}", resourceName, qps, dimension, e.getMessage());
            throw new RuntimeException("请求过于频繁，请稍后再试", e);
        } catch (Exception e) {
            Tracer.trace(e);
            throw e;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 构建限流资源名（按维度）
     */
    private String buildResourceName(ProceedingJoinPoint point, RateLimiter.Dimension dimension) {
        String baseName = point.getTarget().getClass().getName() + "." + point.getSignature().getName();
        switch (dimension) {
            case IP:
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String ip = IpUtils.getClientIp(request);
                    return baseName + "_IP:" + ip;
                }
                return baseName + "_IP:unknown";
            case USER:
                // 需根据实际业务替换：获取当前登录用户ID/账号
                String userId = getCurrentUserId();
                return baseName + "_USER:" + (userId != null ? userId : "anonymous");
            case GLOBAL:
            default:
                return baseName + "_GLOBAL";
        }
    }

    /**
     * 初始化Sentinel限流规则
     */
    private void initFlowRule(String resourceName, double qps, RateLimiter.Mode mode) {
        FlowRule rule = new FlowRule();
        rule.setResource(resourceName);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);

        // 设置限流模式
        switch (mode) {
            case WARM_UP:
                rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
                rule.setWarmUpPeriodSec(loggingProperties.getRateLimit().getWarmUpPeriod());
                break;
            case RATE_LIMITER:
                rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
                rule.setMaxQueueingTimeMs(loggingProperties.getRateLimit().getMaxWaitTime());
                break;
            case DEFAULT:
            default:
                rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
                break;
        }

        // 规则注册（追加方式，避免覆盖已有规则）
        if (FlowRuleManager.getRules().stream().noneMatch(r -> r.getResource().equals(resourceName))) {
            List<FlowRule> existingRules = new ArrayList<>(FlowRuleManager.getRules());
            existingRules.add(rule);
            FlowRuleManager.loadRules(existingRules);
            registeredResources.add(resourceName);
        }
    }



    /**
     * 获取当前登录用户ID
     *
     * <p>通过 {@link UserIdResolver} SPI 获取，默认实现返回 null（匿名用户）。</p>
     */
    private String getCurrentUserId() {
        return userIdResolver.getCurrentUserId();
    }

    /**
     * Bean 销毁时清理本切面注册的 Sentinel 规则
     */
    @PreDestroy
    public void cleanup() {
        List<FlowRule> remaining = FlowRuleManager.getRules().stream()
                .filter(r -> !registeredResources.contains(r.getResource()))
                .collect(Collectors.toList());
        FlowRuleManager.loadRules(remaining);
        registeredResources.clear();
    }
}