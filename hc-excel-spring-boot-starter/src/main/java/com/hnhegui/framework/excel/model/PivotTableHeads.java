package com.hnhegui.framework.excel.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 列转行表头组合（固定列 + 动态列 + 汇总列）
 * 
 * <p>用于构建包含固定列、动态列和汇总列的复杂表头结构。</p>
 * 
 * <h3>表头结构示例：</h3>
 * <pre>
 * | 固定列1 | 固定列2 | 动态列1 | 动态列2 | ... | 动态列N | 汇总列 |
 * |   市场  |   区域  |  产品A  |  产品B  | ... |  产品N  |  合计  |
 * </pre>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 方式1：链式构建
 * PivotTableHeads heads = PivotTableHeads.builder()
 *     .fixedColumns(Arrays.asList(
 *         PivotTableHead.fixed("市场", "market"),
 *         PivotTableHead.fixed("区域", "region")
 *     ))
 *     .dynamicColumns(productList.stream()
 *         .map(p -> PivotTableHead.dynamic(p.getName(), "product_" + p.getId()))
 *         .collect(Collectors.toList()))
 *     .summaryColumn(PivotTableHead.summary("合计", "total"))
 *     .build();
 * 
 * // 方式2：快速构建
 * PivotTableHeads heads = PivotTableHeads.of(
 *     Arrays.asList("市场", "区域"),                    // 固定列表头名
 *     Arrays.asList("market", "region"),              // 固定列字段名
 *     Arrays.asList("产品A", "产品B", "产品C"),         // 动态列表头名
 *     Arrays.asList("product_a", "product_b", "product_c"), // 动态列字段名
 *     "合计",                                         // 汇总列表头名
 *     "total"                                         // 汇总列字段名
 * );
 * 
 * // 获取完整表头列表
 * List<DynamicHead> allHeads = heads.toDynamicHeads();
 * 
 * // 获取各部分表头
 * List<DynamicHead> fixedHeads = heads.getFixedDynamicHeads();
 * List<DynamicHead> dynamicHeads = heads.getDynamicDynamicHeads();
 * List<DynamicHead> summaryHeads = heads.getSummaryDynamicHeads();
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@Builder
public class PivotTableHeads {

    /**
     * 固定列（左侧）
     */
    private List<PivotTableHead> fixedColumns;

    /**
     * 动态列（中间）
     */
    private List<PivotTableHead> dynamicColumns;

    /**
     * 汇总列（右侧）
     */
    private PivotTableHead summaryColumn;

    /**
     * 快速构建列转行表头
     * 
     * <p>通过简单的名称和字段列表快速构建完整的表头结构。</p>
     *
     * @param fixedNames 固定列表头名称列表
     * @param fixedFields 固定列字段名列表
     * @param dynamicNames 动态列表头名称列表
     * @param dynamicFields 动态列字段名列表
     * @param summaryName 汇总列表头名称
     * @param summaryField 汇总列字段名
     * @return 完整的列转行表头
     */
    public static PivotTableHeads of(
            List<String> fixedNames,
            List<String> fixedFields,
            List<String> dynamicNames,
            List<String> dynamicFields,
            String summaryName,
            String summaryField) {
        
        // 构建固定列
        List<PivotTableHead> fixedColumns = new ArrayList<>();
        if (fixedNames != null && fixedFields != null) {
            for (int i = 0; i < Math.min(fixedNames.size(), fixedFields.size()); i++) {
                fixedColumns.add(PivotTableHead.fixed(fixedNames.get(i), fixedFields.get(i)));
            }
        }
        
        // 构建动态列
        List<PivotTableHead> dynamicColumns = new ArrayList<>();
        if (dynamicNames != null && dynamicFields != null) {
            for (int i = 0; i < Math.min(dynamicNames.size(), dynamicFields.size()); i++) {
                dynamicColumns.add(PivotTableHead.dynamic(dynamicNames.get(i), dynamicFields.get(i)));
            }
        }
        
        // 构建汇总列
        PivotTableHead summaryColumn = null;
        if (summaryName != null && summaryField != null) {
            summaryColumn = PivotTableHead.summary(summaryName, summaryField);
        }
        
        return PivotTableHeads.builder()
                .fixedColumns(fixedColumns)
                .dynamicColumns(dynamicColumns)
                .summaryColumn(summaryColumn)
                .build();
    }

    /**
     * 快速构建（仅固定列+动态列）
     *
     * @param fixedNames 固定列表头名称列表
     * @param fixedFields 固定列字段名列表
     * @param dynamicNames 动态列表头名称列表
     * @param dynamicFields 动态列字段名列表
     * @return 完整的列转行表头
     */
    public static PivotTableHeads of(
            List<String> fixedNames,
            List<String> fixedFields,
            List<String> dynamicNames,
            List<String> dynamicFields) {
        return of(fixedNames, fixedFields, dynamicNames, dynamicFields, null, null);
    }

    /**
     * 创建空表头构建器
     *
     * @return 构建器
     */
    public static PivotTableHeadsBuilder builder() {
        return new PivotTableHeadsBuilder();
    }

    /**
     * 获取固定列数量
     *
     * @return 固定列数量
     */
    public int getFixedColumnCount() {
        return fixedColumns != null ? fixedColumns.size() : 0;
    }

    /**
     * 获取动态列数量
     *
     * @return 动态列数量
     */
    public int getDynamicColumnCount() {
        return dynamicColumns != null ? dynamicColumns.size() : 0;
    }

    /**
     * 获取总列数
     *
     * @return 总列数
     */
    public int getTotalColumnCount() {
        int count = getFixedColumnCount() + getDynamicColumnCount();
        if (summaryColumn != null) {
            count += 1;
        }
        return count;
    }

    /**
     * 转换为DynamicHead列表（完整表头）
     * 
     * <p>按顺序：固定列 -> 动态列 -> 汇总列</p>
     *
     * @return DynamicHead列表
     */
    public List<DynamicHead> toDynamicHeads() {
        List<DynamicHead> heads = new ArrayList<>();
        
        // 添加固定列
        if (fixedColumns != null) {
            heads.addAll(fixedColumns.stream()
                    .map(PivotTableHead::toDynamicHead)
                    .toList());
        }
        
        // 添加动态列
        if (dynamicColumns != null) {
            heads.addAll(dynamicColumns.stream()
                    .map(PivotTableHead::toDynamicHead)
                    .toList());
        }
        
        // 添加汇总列
        if (summaryColumn != null) {
            heads.add(summaryColumn.toDynamicHead());
        }
        
        return heads;
    }

    /**
     * 获取固定列的DynamicHead列表
     *
     * @return 固定列DynamicHead列表
     */
    public List<DynamicHead> getFixedDynamicHeads() {
        if (fixedColumns == null) {
            return Collections.emptyList();
        }
        return fixedColumns.stream()
                .map(PivotTableHead::toDynamicHead)
                .collect(Collectors.toList());
    }

    /**
     * 获取动态列的DynamicHead列表
     *
     * @return 动态列DynamicHead列表
     */
    public List<DynamicHead> getDynamicDynamicHeads() {
        if (dynamicColumns == null) {
            return Collections.emptyList();
        }
        return dynamicColumns.stream()
                .map(PivotTableHead::toDynamicHead)
                .collect(Collectors.toList());
    }

    /**
     * 获取汇总列的DynamicHead
     *
     * @return 汇总列DynamicHead，无汇总列时返回null
     */
    public DynamicHead getSummaryDynamicHead() {
        return summaryColumn != null ? summaryColumn.toDynamicHead() : null;
    }

    /**
     * 获取固定列字段名列表
     *
     * @return 字段名列表
     */
    public List<String> getFixedFields() {
        if (fixedColumns == null) {
            return Collections.emptyList();
        }
        return fixedColumns.stream()
                .map(PivotTableHead::getField)
                .collect(Collectors.toList());
    }

    /**
     * 获取动态列字段名列表
     *
     * @return 字段名列表
     */
    public List<String> getDynamicFields() {
        if (dynamicColumns == null) {
            return Collections.emptyList();
        }
        return dynamicColumns.stream()
                .map(PivotTableHead::getField)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有字段名列表（按顺序）
     *
     * @return 字段名列表
     */
    public List<String> getAllFields() {
        List<String> fields = new ArrayList<>();
        fields.addAll(getFixedFields());
        fields.addAll(getDynamicFields());
        if (summaryColumn != null) {
            fields.add(summaryColumn.getField());
        }
        return fields;
    }
}
