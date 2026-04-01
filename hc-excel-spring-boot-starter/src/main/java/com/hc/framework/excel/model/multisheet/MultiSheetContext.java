package com.hc.framework.excel.model.multisheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多Sheet导入上下文
 * 用于在各Sheet之间传递数据和共享状态
 */
public class MultiSheetContext {

    /**
     * 上下文数据存储
     */
    private final Map<String, Object> contextData = new HashMap<>();

    /**
     * 已导入的Sheet数据缓存（key: sheetName）
     */
    private final Map<String, List<?>> sheetDataCache = new HashMap<>();

    /**
     * 设置上下文数据
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, Object value) {
        contextData.put(key, value);
    }

    /**
     * 获取上下文数据
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) contextData.get(key);
    }

    /**
     * 获取上下文数据（带默认值）
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) contextData.getOrDefault(key, defaultValue);
    }

    /**
     * 缓存Sheet数据
     *
     * @param sheetName Sheet名称
     * @param data      数据列表
     */
    public void cacheSheetData(String sheetName, List<?> data) {
        sheetDataCache.put(sheetName, data);
    }

    /**
     * 获取缓存的Sheet数据
     *
     * @param sheetName Sheet名称
     * @param <T>       数据类型
     * @return 数据列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getSheetData(String sheetName) {
        return (List<T>) sheetDataCache.get(sheetName);
    }

    /**
     * 检查是否存在指定Sheet的数据
     *
     * @param sheetName Sheet名称
     * @return 是否存在
     */
    public boolean hasSheetData(String sheetName) {
        return sheetDataCache.containsKey(sheetName);
    }

    /**
     * 检查是否存在指定键的数据
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean containsKey(String key) {
        return contextData.containsKey(key);
    }

    /**
     * 清空上下文
     */
    public void clear() {
        contextData.clear();
        sheetDataCache.clear();
    }
}
