package com.hc.framework.redis.core;

import com.hc.framework.redis.testdto.TestUserDto;
import com.your.business.dto.BizOrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.exc.InvalidTypeIdException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 反序列化多态白名单单测：
 * - 默认白名单（1.1 场景 A：含 @class 指向 javax.naming.Reference / TemplatesImpl 的 JSON 被拒绝）
 * - 业务包追加 / 漏配 java.util. 场景（1.2 两个场景）
 * - 白名单 DTO 往返（1.3，嵌套集合 + LocalDateTime，类型元数据不丢失）
 */
class CustomGenericJackson2JsonRedisSerializerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 3, 10, 30, 0);

    /**
     * 构造含任意类型 id（WrapperArray 形式）的 JSON 字节，模拟被篡改/注入的缓存值。
     */
    private static byte[] typedJson(String typeId) {
        return ("[\"" + typeId + "\",{}]").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("默认白名单拒绝 javax.naming.Reference / TemplatesImpl 等危险类型且不实例化")
    void rejectsDangerousTypesOutOfWhitelist() {
        CustomGenericJackson2JsonRedisSerializer serializer = new CustomGenericJackson2JsonRedisSerializer();
        List<String> dangerousTypes = List.of(
            "javax.naming.Reference",
            "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"
        );
        for (String dangerousType : dangerousTypes) {
            SerializationException exception = assertThrows(SerializationException.class,
                () -> serializer.deserialize(typedJson(dangerousType)),
                "应拒绝类型: " + dangerousType);
            // 根因必须是 Jackson 类型校验异常（InvalidTypeIdException），说明白名单拦截发生在实例化之前
            Throwable cause = exception.getCause();
            assertNotNull(cause, "反序列化异常必须携带 cause");
            assertInstanceOf(InvalidTypeIdException.class, cause, "期望 InvalidTypeIdException 拦截 " + dangerousType + "，实际: " + cause.getClass().getName()
                + " cause=" + cause.getMessage());
        }
    }

    @Test
    @DisplayName("默认白名单内的 DTO（嵌套集合 + LocalDateTime）序列化-反序列化往返一致")
    void roundTripDtoInDefaultWhitelist() {
        CustomGenericJackson2JsonRedisSerializer serializer = new CustomGenericJackson2JsonRedisSerializer();
        TestUserDto dto = new TestUserDto(1L, "张三", List.of("vip", "new"), NOW);

        byte[] bytes = serializer.serialize(dto);
        Object result = serializer.deserialize(bytes);

        assertInstanceOf(TestUserDto.class, result, "类型元数据必须保留");
        assertEquals(dto, result, "往返内容必须一致（含嵌套集合与 LocalDateTime）");
    }

    @Test
    @DisplayName("配置业务包后该包 DTO 往返一致；默认白名单仍生效")
    void roundTripDtoWithConfiguredBusinessPackage() {
        CustomGenericJackson2JsonRedisSerializer serializer =
            new CustomGenericJackson2JsonRedisSerializer(List.of("com.your.business.dto."));
        BizOrderDto dto = new BizOrderDto("NO-20260903-001", List.of("SKU-A", "SKU-B"), NOW);

        byte[] bytes = serializer.serialize(dto);
        Object result = serializer.deserialize(bytes);

        assertInstanceOf(BizOrderDto.class, result, "配置包类型必须可反序列化");
        assertEquals(dto, result);
    }

    @Test
    @DisplayName("业务配置漏配 java.util. 时集合类型仍可反序列化（默认白名单并集兜底）")
    void listRoundTripWhenJavaUtilNotConfigured() {
        // 业务只配了自身包，故意漏掉 java.util.——默认白名单并集仍应放行集合类型
        CustomGenericJackson2JsonRedisSerializer serializer =
            new CustomGenericJackson2JsonRedisSerializer(List.of("com.your.business.dto."));

        ArrayList<String> list = new ArrayList<>(List.of("a", "b", "c"));
        byte[] bytes = serializer.serialize(list);
        Object result = serializer.deserialize(bytes);

        assertInstanceOf(List.class, result);
        assertEquals(list, result);
    }

    @Test
    @DisplayName("默认白名单（未配置扩展包）拒绝业务包 DTO 的类型 id")
    void rejectsBusinessTypeWithDefaultOnlyWhitelist() {
        CustomGenericJackson2JsonRedisSerializer serializer = new CustomGenericJackson2JsonRedisSerializer();
        // 构造的是被篡改后的缓存字节：类型指向默认白名单外的 com.your.business.dto.BizOrderDto
        SerializationException exception = assertThrows(SerializationException.class,
            () -> serializer.deserialize(typedJson("com.your.business.dto.BizOrderDto")));
        assertInstanceOf(InvalidTypeIdException.class, exception.getCause());
    }

    @Test
    @DisplayName("配置前缀不带结尾点号时自动归一化（com.your.business 追加为 com.your.business.）")
    void packagePrefixNormalizedWithTrailingDot() {
        List<String> merged = CustomGenericJackson2JsonRedisSerializer.mergeAllowedPackages(
            List.of("com.your.business"));
        assertTrue(merged.contains("com.your.business."), "缺省结尾点号的前缀应归一化，实际: " + merged);
        // 默认白名单前缀仍保留
        for (String prefix : CustomGenericJackson2JsonRedisSerializer.DEFAULT_ALLOWED_PACKAGES) {
            assertTrue(merged.contains(prefix), "默认白名单前缀不应丢失: " + prefix);
        }
    }
}
