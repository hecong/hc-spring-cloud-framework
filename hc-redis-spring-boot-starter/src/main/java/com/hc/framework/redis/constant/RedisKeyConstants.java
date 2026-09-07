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

    /**
     * 每日自增序列前缀（值保留既有字面量，避免变更历史 key）
     */
    public static final String SEQ = "seq:";

    /**
     * 永久自增序列前缀（值保留既有字面量，避免变更历史 key）
     */
    public static final String SEQ_GLOBAL = "seq:global:";

    /**
     * 新增 key 前缀命名规范：hc:{模块}:{业务}（模块与业务均为小写字母与连字符）。
     * 存量前缀（如 {@link #SEQ}、{@link #SEQ_GLOBAL}）为兼容线上数据保留原值，
     * 仅在新增前缀时遵循本规范。
     */
    private RedisKeyConstants() {
        // 私有构造，防止实例化
    }
}
