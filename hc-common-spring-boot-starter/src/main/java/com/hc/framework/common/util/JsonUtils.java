package com.hc.framework.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * JSON 序列化/反序列化工具类
 *
 * <p>基于 Jackson 3.x 实现，内置单例 {@link ObjectMapper}，支持 Java 8 时间类型（LocalDateTime 等）。</p>
 *
 * <p>配置说明：</p>
 * <ul>
 *     <li>忽略 JSON 中的未知字段（{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} = false）</li>
 *     <li>null 字段不参与序列化</li>
 *     <li>Java 8 时间模块已内置到 Jackson 3.x databind，无需单独引入</li>
 * </ul>
 *
 * @author hc-framework
 */
public class JsonUtils {

    private static final ObjectMapper MAPPER = buildMapper();

    private JsonUtils() {
    }

    private static ObjectMapper buildMapper() {
        return JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    // ==================== 序列化 ====================

    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    public static String toPrettyJson(Object object) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    // ==================== 反序列化 ====================

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> itemClass) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, itemClass));
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 反序列化为 List 失败: " + e.getMessage(), e);
        }
    }

    // ==================== Map 操作 ====================

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 转 Map 失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return MAPPER.convertValue(object, Map.class);
    }

    public static <T> T fromMap(Map<?, ?> map, Class<T> clazz) {
        return MAPPER.convertValue(map, clazz);
    }

    // ==================== JSON 节点 ====================

    public static String getNodeAsText(String json, String fieldName) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode field = node.get(fieldName);
            return field != null ? field.asText() : null;
        } catch (JacksonException e) {
            throw new RuntimeException("JSON 节点读取失败: " + e.getMessage(), e);
        }
    }

    public static boolean isValidJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }
}
