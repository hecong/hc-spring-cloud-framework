package com.hc.framework.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author hecong
 * @since 2026/4/1 17:20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {

    /**
     * 防重复时间（秒）
     */
    int expire() default 1;

    /**
     * 支持 SPEL 表达式
     */
    String key() default "";

    /**
     * 提示语
     */
    String message() default "操作过于频繁，请稍后再试";
}