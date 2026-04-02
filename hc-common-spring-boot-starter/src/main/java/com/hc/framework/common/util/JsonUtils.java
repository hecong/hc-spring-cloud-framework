package com.hc.framework.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

/**
 * JSON 序列化/反序列化工具类
 *
 * <p>基于 Jackson 实现，内置单例 {@link ObjectMapper}，支持 Java 8 时间类型（LocalDateTime 等）。</p>
 *
 * <p>配置说明：</p>
 * <ul>
 *     <li>忽略 JSON 中的未知字段（{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} = false）</li>
 *     <li>null 字段不参与序列化（{@link JsonInclude#NON_NULL}）</li>
 *     <li>日期不序列化为时间戳（{@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} = false）</li>
 *     <li>支持 Java 8 时间模块（{@link JavaTimeModule}）</li>
 * </ul>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 * // 序列化
 * String json = JsonUtils.toJson(user);
 *
 * // 反序列化
 * User user = JsonUtils.fromJson(json, User.class);
 *
 * // 泛型反序列化
 * List<User> users = JsonUtils.fromJsonList(jsonArray, User.class);
 * Map<String, Object> map = JsonUtils.toMap(json);
 *
 * // 获取节点值
 * String name = JsonUtils.getNodeAsText(json, "name");
 * }</pre>
 *
 * @author hc-framework
 */
public class JsonUtils {

    /**
     * 内置 ObjectMapper 单例（线程安全）
     */
    private static final ObjectMapper MAPPER = buildMapper();

    private JsonUtils() {
    }

    /**
     * 构建 ObjectMapper，统一配置
     */
    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块，支持 LocalDateTime 等
        mapper.registerModule(new JavaTimeModule());
        // 日期不序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略 JSON 中的未知字段
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // null 字段不序列化
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 获取内置 ObjectMapper（如需自定义配置可直接操作）
     *
     * @return ObjectMapper 实例
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    // ==================== 序列化 ====================

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param object 待序列化对象，为 null 时返回 "null"
     * @return JSON 字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将对象序列化为格式化（美化）的 JSON 字符串
     *
     * @param object 待序列化对象
     * @return 格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object object) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败: " + e.getMessage(), e);
        }
    }

    // ==================== 反序列化 ====================

    /**
     * 将 JSON 字符串反序列化为指定类型对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化结果，json 为空时返回 null
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为复杂泛型对象
     *
     * <pre>{@code
     * List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>(){});
     * Map<String, List<User>> map = JsonUtils.fromJson(json, new TypeReference<>(){});
     * }</pre>
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           目标类型泛型
     * @return 反序列化结果，json 为空时返回 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 JSON 数组字符串反序列化为 List
     *
     * @param json      JSON 数组字符串，例如 "[{...},{...}]"
     * @param itemClass List 元素类型
     * @param <T>       元素类型泛型
     * @return List 结果，json 为空时返回空 List
     */
    public static <T> List<T> fromJsonList(String json, Class<T> itemClass) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, itemClass));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化为 List 失败: " + e.getMessage(), e);
        }
    }

    // ==================== Map 操作 ====================

    /**
     * 将 JSON 字符串转换为 Map
     *
     * @param json JSON 对象字符串，例如 "{\"key\":\"value\"}"
     * @return Map，json 为空时返回空 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 转 Map 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将对象转换为 Map（通过 JSON 中转）
     *
     * @param object 待转换对象
     * @return Map 结构
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return MAPPER.convertValue(object, Map.class);
    }

    /**
     * 将 Map 转换为指定类型对象
     *
     * @param map   Map 数据
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换结果
     */
    public static <T> T fromMap(Map<?, ?> map, Class<T> clazz) {
        return MAPPER.convertValue(map, clazz);
    }

    // ==================== JSON 节点 ====================

    /**
     * 获取 JSON 字符串中指定节点的文本值
     *
     * <pre>{@code
     * String name = JsonUtils.getNodeAsText("{\"name\":\"Tom\",\"age\":18}", "name"); // "Tom"
     * }</pre>
     *
     * @param json      JSON 字符串
     * @param fieldName 字段名
     * @return 字段值字符串，字段不存在时返回 null
     */
    public static String getNodeAsText(String json, String fieldName) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode field = node.get(fieldName);
            return field != null ? field.asText() : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 节点读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断字符串是否是合法的 JSON
     *
     * @param json 待验证字符串
     * @return true 表示是合法 JSON
     */
    public static boolean isValidJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
