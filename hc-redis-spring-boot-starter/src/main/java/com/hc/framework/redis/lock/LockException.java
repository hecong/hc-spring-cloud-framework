package com.hc.framework.redis.lock;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 业务逻辑异常 Exception
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public final class LockException extends RuntimeException {

    /**
     * 锁获取失败（通用错误）
     */
    public static final int LOCK_ACQUIRE_FAILED = 1001;
    
    /**
     * 锁获取超时
     */
    public static final int LOCK_ACQUIRE_TIMEOUT = 1002;
    
    /**
     * 锁被占用
     */
    public static final int LOCK_BUSY = 1003;

    /**
     * 持锁执行业务逻辑时线程被中断
     */
    public static final int LOCK_INTERRUPTED = 1004;

    /**
     * 持锁执行业务逻辑失败（回调抛出受检异常等）
     */
    public static final int LOCK_EXECUTION_FAILED = 1005;

    /**
     * 业务错误码
     */
    private Integer code;

    private boolean isError;

    /**
     * 空构造方法，避免反序列化问题
     */
    public LockException() {
    }

    public LockException(String message) {
        super(message);
        this.code = 500;
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public LockException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public LockException(String message, boolean isError) {
        super(message);
        this.code = 500;
        this.isError = isError;
    }

    public LockException(int code, String message, boolean isError) {
        super(message);
        this.code = code;
        this.isError = isError;
    }

    public LockException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}