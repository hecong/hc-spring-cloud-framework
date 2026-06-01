package com.hc.framework.common.exception;

import java.io.Serial;

/**
 * 重复提交异常
 *
 * <p>当检测到重复提交时抛出此异常。HTTP 状态码 429 (Too Many Requests)。</p>
 * <p>由 hc-web 的 {@code GlobalExceptionHandler} 统一处理。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class RepeatSubmitException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int HTTP_CODE = 429;

    public RepeatSubmitException(String message) {
        super(message);
    }
}
