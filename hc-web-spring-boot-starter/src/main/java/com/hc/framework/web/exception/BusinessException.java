package com.hc.framework.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 业务异常默认错误码：400（BAD_REQUEST 语义，参数不合法/状态冲突等归属 4xx；
     * 需要显式错误码时使用两参/三参构造）
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST.value(), message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
