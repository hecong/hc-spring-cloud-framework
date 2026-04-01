package com.hc.framework.redis.lock;

/**
 * @author hecong
 * @since 2026/4/1 15:53
 */
public enum LockType {

    /*
     * Redis可重入锁（默认）
     */
    REDIS_REENTRANT_LOCK,
    /*
     * Redis公平锁
     */
    REDIS_FAIR_LOCK,
    /*
     * Redis自旋锁
     */
    REDIS_SPIN_LOCK,
    /*
     * Redis读锁
     */
    REDIS_READ_LOCK,
    /*
     * Redis写锁
     */
    REDIS_WRITE_LOCK,
    ;
}
