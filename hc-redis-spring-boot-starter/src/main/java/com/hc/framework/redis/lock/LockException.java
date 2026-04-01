package com.hc.framework.redis.lock;

import cn.hutool.core.util.StrUtil;
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
}