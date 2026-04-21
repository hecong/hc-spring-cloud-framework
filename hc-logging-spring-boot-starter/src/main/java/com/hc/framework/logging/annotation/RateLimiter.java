package com.hc.framework.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * 限流QPS，默认100
     */
    double qps() default 100.0;

    /**
     * 限流模式
     */
    Mode mode() default Mode.DEFAULT;

    /**
     * 限流维度
     */
    Dimension dimension() default Dimension.GLOBAL;

    /**
     * 限流模式
     */
    enum Mode {
        /**
         * 默认快速失败
         */
        DEFAULT,
        /**
         * 预热模式
         */
        WARM_UP,
        /**
         * 匀速排队
         */
        RATE_LIMITER
    }

    /**
     * 限流维度
     */
    enum Dimension {
        /**
         * 全局限流
         */
        GLOBAL,
        /**
         * 按IP限流
         */
        IP,
        /**
         * 按用户限流
         */
        USER
    }
}
