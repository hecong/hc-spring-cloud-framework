package com.hc.framework.redis.constant;

/**
 * Redis Key 常量
 *
 * @author hecong
 */
public final class RedisKeyConstants {

    /**
     * 防重复提交前缀
     */
    public static final String REPEAT = "repeat:submit:";

    private RedisKeyConstants() {
        // 私有构造，防止实例化
    }
}
