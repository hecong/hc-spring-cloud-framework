package com.hc.framework.redis.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;
import org.dromara.hutool.core.date.DatePattern;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * 自定义 Redis JSON 序列化器
 * 解决：null值忽略、反序列化容错、类型保留
 *
 * <p>已适配 Spring Boot 4 / Jackson 3.x：使用 JsonMapper.builder() 构建。</p>
 * <p>Java 8 时间类型（LocalDateTime 等）由 Jackson 3.x 内置支持，默认输出 ISO-8601 格式。</p>
 * <p>不再继承 GenericJackson2JsonRedisSerializer（该类仍依赖 Jackson 2.x），
 * 改为直接实现 {@link RedisSerializer}，使用 Jackson 3.x 的 {@link ObjectMapper}。</p>
 *
 * @author hecong
 * @since 2026/4/1
 */
public class CustomGenericJackson2JsonRedisSerializer implements RedisSerializer<Object> {

    private static final ObjectMapper MAPPER = buildObjectMapper();

    public CustomGenericJackson2JsonRedisSerializer() {
    }

    private static ObjectMapper buildObjectMapper() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .build();

        return JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat(DatePattern.NORM_DATETIME_PATTERN))
            .defaultTimeZone(TimeZone.getDefault())
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .activateDefaultTyping(validator, DefaultTyping.NON_FINAL)
            .build();
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new SerializationException("Could not serialize: " + e.getMessage(), e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return MAPPER.readValue(bytes, Object.class);
        } catch (JacksonException e) {
            throw new SerializationException("Could not deserialize: " + e.getMessage(), e);
        }
    }
}
