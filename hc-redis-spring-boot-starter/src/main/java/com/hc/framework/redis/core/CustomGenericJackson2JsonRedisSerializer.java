package com.hc.framework.redis.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.dromara.hutool.core.date.DatePattern;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * 自定义 Redis JSON 序列化器
 * 解决：null值忽略、反序列化容错、类型保留、多态类型白名单（防反序列化 RCE）
 *
 * <p>已适配 Spring Boot 4 / Jackson 3.x：使用 JsonMapper.builder() 构建。</p>
 * <p>Java 8 时间类型（LocalDateTime 等）由 Jackson 3.x 内置支持，默认输出 ISO-8601 格式。</p>
 * <p>不再继承 GenericJackson2JsonRedisSerializer（该类仍依赖 Jackson 2.x），
 * 改为直接实现 {@link RedisSerializer}，使用 Jackson 3.x 的 {@link ObjectMapper}。</p>
 * <p><b>安全说明：</b>多态反序列化仅放行白名单内包前缀的类型（{@link #DEFAULT_ALLOWED_PACKAGES}
 * 与配置的 {@code hc.redis.allowed-packages} 取并集），白名单外 {@code @class} / 类型 id
 * 在反序列化时抛出类型校验异常，不会被实例化。</p>
 *
 * @author hecong
 * @since 2026/4/1
 */
@NullMarked
public class CustomGenericJackson2JsonRedisSerializer implements RedisSerializer<Object> {

    /**
     * 默认多态白名单包前缀（始终生效，业务配置仅追加不替换）
     */
    public static final List<String> DEFAULT_ALLOWED_PACKAGES = List.of(
        "com.hc.framework.", "com.hnhegui.", "java.util.", "java.lang.", "java.time."
    );

    /**
     * 默认实例对应的 Mapper（无参构造复用，行为与单例兼容）
     */
    private static final ObjectMapper DEFAULT_MAPPER = buildMapper(DEFAULT_ALLOWED_PACKAGES);

    /**
     * 当前实例 Mapper（每个实例按自身白名单构建一次并复用）
     */
    private final ObjectMapper mapper;

    public CustomGenericJackson2JsonRedisSerializer() {
        this.mapper = DEFAULT_MAPPER;
    }

    /**
     * @param allowedPackages 业务追加的多态白名单包前缀（与默认白名单取并集）
     */
    public CustomGenericJackson2JsonRedisSerializer(List<String> allowedPackages) {
        this.mapper = buildMapper(mergeAllowedPackages(allowedPackages));
    }

    /**
     * 将业务配置的白名单与默认白名单取并集；前缀缺失结尾 {@code .} 时自动补全归一化。
     */
    static List<String> mergeAllowedPackages(List<String> allowedPackages) {
        List<String> merged = new ArrayList<>(DEFAULT_ALLOWED_PACKAGES);
        for (String packagePrefix : allowedPackages) {
            if (packagePrefix.isBlank()) {
                continue;
            }
            String normalized = packagePrefix.trim();
            if (!normalized.endsWith(".")) {
                normalized += ".";
            }
            if (!merged.contains(normalized)) {
                merged.add(normalized);
            }
        }
        return List.copyOf(merged);
    }

    private static ObjectMapper buildMapper(List<String> allowedPackages) {
        BasicPolymorphicTypeValidator.Builder validatorBuilder = BasicPolymorphicTypeValidator.builder();
        for (String packagePrefix : allowedPackages) {
            validatorBuilder.allowIfSubType(packagePrefix);
        }
        PolymorphicTypeValidator validator = validatorBuilder.build();

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
    public byte[] serialize(@Nullable Object value) throws SerializationException {
        if (value == null) {
            // RedisSerializer（spring-data-redis 4.x）契约：可返回空数组，但不允许返回 null
            return new byte[0];
        }
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JacksonException e) {
            throw new SerializationException("Could not serialize: " + e.getMessage(), e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(bytes, Object.class);
        } catch (JacksonException e) {
            throw new SerializationException("Could not deserialize: " + e.getMessage(), e);
        }
    }
}
