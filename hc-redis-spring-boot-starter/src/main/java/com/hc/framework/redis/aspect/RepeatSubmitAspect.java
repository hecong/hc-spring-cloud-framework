package com.hc.framework.redis.aspect;

import cn.hutool.core.util.StrUtil;
import com.hc.framework.redis.annotation.RepeatSubmit;
import com.hc.framework.redis.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author hecong
 * @since 2026/4/1 17:21
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REPEAT_PREFIX = RedisKeyConstants.REPEAT;
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();


    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        // 1. 解析 SPEL Key
        String key = generateKey(point, repeatSubmit);
        String redisKey = REPEAT_PREFIX + key;

        // 2. 尝试占锁
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "1", repeatSubmit.expire(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            throw new RuntimeException(repeatSubmit.message());
        }

        // 3. 执行方法
        return point.proceed();
    }

    /**
     * 生成防重复KEY（支持SPEL）
     */
    private String generateKey(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) {
        if (StrUtil.isNotBlank(repeatSubmit.key())) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", point.getArgs());
            context.setVariable("target", point.getTarget());
            return SPEL_PARSER.parseExpression(repeatSubmit.key()).getValue(context, String.class);
        }
        // 默认：类名+方法名
        MethodSignature signature = (MethodSignature) point.getSignature();
        return point.getTarget().getClass().getName() + ":" + signature.getMethod().getName();
    }
}