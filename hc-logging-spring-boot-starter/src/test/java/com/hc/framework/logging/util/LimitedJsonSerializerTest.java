package com.hc.framework.logging.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 限长 JSON 序列化单测：超限中止返回截断标记，阈值内完整输出
 */
class LimitedJsonSerializerTest {

    @Test
    @DisplayName("超长对象：序列化被提前终止，仅返回 <truncated, len=N> 且不含超限内容")
    void overLimitReturnsTruncationMarker() {
        String huge = "x".repeat(100_000);
        Map<String, Object> value = new HashMap<>();
        value.put("big", huge);
        value.put("small", "ok");

        String result = LimitedJsonSerializer.toLimitedJson(value, 4096);

        assertTrue(result.startsWith("<truncated, len="), result);
        assertFalse(result.contains("xxxx"), "不得包含超限对象的完整内容");
        assertFalse(result.contains("\"big\""), "超限对象键值不得部分暴露");
    }

    @Test
    @DisplayName("阈值内对象：完整 JSON 输出，边界内容不受损")
    void underLimitReturnsFullJson() {
        Map<String, Object> value = new HashMap<>();
        value.put("name", "hc");
        value.put("age", 3);

        String result = LimitedJsonSerializer.toLimitedJson(value, 4096);

        assertTrue(result.contains("\"name\":\"hc\""), result);
        assertTrue(result.contains("\"age\":3"), result);
        assertFalse(result.contains("truncated"));
    }

    @Test
    @DisplayName("null 与非法上限边界：null 输出 null，非正上限直接截断")
    void boundaryCases() {
        assertEquals("null", LimitedJsonSerializer.toLimitedJson(null, 4096));
        String marker = LimitedJsonSerializer.toLimitedJson(Map.of("a", "b"), 0);
        assertEquals("<truncated, len=0>", marker);
        assertEquals("<truncated, len=0>", LimitedJsonSerializer.toLimitedJson(Map.of("a", "b"), -1));
    }
}
