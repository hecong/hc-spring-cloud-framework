package com.hc.framework.excel.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 动态表头定义
 */
@Data
@Builder
public class DynamicHead {

    /**
     * 表头名称
     */
    private String name;

    /**
     * 字段名（用于数据映射）
     */
    private String field;

    /**
     * 列宽
     */
    private Integer width;

    /**
     * 格式化（日期格式等）
     */
    private String format;

    /**
     * 创建单级表头
     */
    public static DynamicHead of(String name, String field) {
        return DynamicHead.builder().name(name).field(field).build();
    }

    /**
     * 创建带宽度的表头
     */
    public static DynamicHead of(String name, String field, Integer width) {
        return DynamicHead.builder().name(name).field(field).width(width).build();
    }

    /**
     * 批量创建表头
     */
    public static List<DynamicHead> of(DynamicHead... heads) {
        return java.util.Arrays.asList(heads);
    }
}
