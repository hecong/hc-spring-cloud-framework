package com.hc.framework.web.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 业务异常错误码语义：单参默认 400，显式错误码不变
 */
class BusinessExceptionTest {

    @Test
    @DisplayName("单参构造默认错误码 400")
    void singleArgDefaultsTo400() {
        BusinessException e = new BusinessException("xx");
        assertEquals(400, e.getCode());
        assertEquals("xx", e.getMessage());
    }

    @Test
    @DisplayName("显式错误码不受影响：500 与自定义码均保留")
    void explicitCodesPreserved() {
        BusinessException serverError = new BusinessException(500, "xx");
        assertEquals(500, serverError.getCode());

        BusinessException custom = new BusinessException(20001, "xx");
        assertEquals(20001, custom.getCode());

        RuntimeException cause = new RuntimeException("boom");
        BusinessException withCause = new BusinessException(500, "xx", cause);
        assertEquals(500, withCause.getCode());
        assertSame(cause, withCause.getCause());
    }
}
