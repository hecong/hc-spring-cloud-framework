package com.hc.framework.web.wrapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CustomizeRequestWrapper：请求体缓存 + 超限降级（已知长度跳过读取 / 未知长度二次防护）
 */
class CustomizeRequestWrapperTest {

    @Test
    @DisplayName("低于默认 2MB 上限：缓存请求体并支持重复读取")
    void cachesBodyWithinLimitAndAllowsReread() throws Exception {
        byte[] content = new byte[1024 * 1024]; // 1MB
        Arrays.fill(content, (byte) 'b');

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(content);

        CustomizeRequestWrapper wrapper = new CustomizeRequestWrapper(request);
        assertArrayEquals(content, wrapper.getBodyBytes(), "1MB 应完整缓存");

        // 重复读取语义：两次读取结果一致
        byte[] first = wrapper.getInputStream().readAllBytes();
        byte[] second = wrapper.getInputStream().readAllBytes();
        assertArrayEquals(content, first);
        assertArrayEquals(content, second);

        // getReader 可用（读取首字符验证）
        BufferedReader reader = wrapper.getReader();
        assertEquals('b', reader.read());
        reader.close();
    }

    @Test
    @DisplayName("Content-Length 明确超过上限（10MB > 2MB）：不读流不缓存")
    void doesNotCacheWhenDeclaredLengthExceedsLimit() throws Exception {
        byte[] content = new byte[10 * 1024 * 1024]; // 10MB
        Arrays.fill(content, (byte) 'a');

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(content); // contentLength 同步为 10MB

        CustomizeRequestWrapper wrapper = new CustomizeRequestWrapper(request);
        assertNull(wrapper.getBodyBytes(), "超限请求体不得被缓存");
        // 降级为直接消费原始流
        byte[] readBack = wrapper.getInputStream().readAllBytes();
        assertArrayEquals(content, readBack, "原始流仍可完整读取");
    }

    @Test
    @DisplayName("自定义更小上限生效（Content-Length 800KB > 100KB → 不缓存）")
    void customLimitApplies() {
        byte[] content = new byte[800 * 1024];
        Arrays.fill(content, (byte) 'c');

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(content);

        CustomizeRequestWrapper wrapper = new CustomizeRequestWrapper(request, 100 * 1024);
        assertNull(wrapper.getBodyBytes());
    }

    @Test
    @DisplayName("Content-Length 未知的二次防护：读入后发现超限则丢弃缓存")
    void secondGuardDropsBodyWhenActualExceedsLimit() {
        byte[] content = new byte[3 * 1024 * 1024]; // 3MB
        Arrays.fill(content, (byte) 'd');

        // 模拟 Content-Length 缺失/被欺骗：始终返回 -1，但流内容真实存在
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1L;
            }
        };
        request.setContent(content);

        CustomizeRequestWrapper wrapper = new CustomizeRequestWrapper(request);
        assertNull(wrapper.getBodyBytes(), "读入后超过上限同样不缓存");
    }

    @Test
    @DisplayName("默认上限常量 = 2MB（其余用例隐式校验）")
    void defaultLimitConstant() {
        assertEquals(2L * 1024 * 1024, CustomizeRequestWrapper.DEFAULT_MAX_CACHED_BODY);
    }
}
