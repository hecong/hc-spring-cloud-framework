package com.hc.framework.excel.model;

import lombok.Builder;
import lombok.Data;

/**
 * 列转行表头定义（固定列 + 动态列 + 汇总列）
 * 
 * <p>适用于动态列表头场景，例如：</p>
 * <pre>
 * | 市场 | 产品1 | 产品2 | 产品3 | ... | 合计 |
 * </pre>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 1. 构建固定列
 * List<PivotTableHead> fixedColumns = Arrays.asList(
 *     PivotTableHead.fixed("市场", "market"),
 *     PivotTableHead.fixed("区域", "region")
 * );
 * 
 * // 2. 构建动态列（通常从数据库查询）
 * List<PivotTableHead> dynamicColumns = productList.stream()
 *     .map(p -> PivotTableHead.dynamic(p.getName(), "product_" + p.getId()))
 *     .collect(Collectors.toList());
 * 
 * // 3. 构建汇总列
 * PivotTableHead summaryColumn = PivotTableHead.summary("合计", "total");
 * 
 * // 4. 组装完整表头
 * PivotTableHeads heads = PivotTableHeads.builder()
 *     .fixedColumns(fixedColumns)
 *     .dynamicColumns(dynamicColumns)
 *     .summaryColumn(summaryColumn)
 *     .build();
 * 
 * // 5. 获取完整表头列表
 * List<DynamicHead> allHeads = heads.toDynamicHeads();
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@Builder
public class PivotTableHead {

    /**
     * 表头名称（显示在Excel中）
     */
    private String name;

    /**
     * 字段名（用于数据映射）
     */
    private String field;

    /**
     * 列宽（字符数）
     */
    private Integer width;

    /**
     * 列类型
     */
    private ColumnType columnType;

    /**
     * 列类型枚举
     */
    public enum ColumnType {
        /**
         * 固定列（左侧固定）
         */
        FIXED,
        /**
         * 动态列（中间动态生成）
         */
        DYNAMIC,
        /**
         * 汇总列（右侧固定）
         */
        SUMMARY
    }

    /**
     * 创建固定列
     * 
     * <p>固定列显示在表头最左侧。</p>
     *
     * @param name 表头名称
     * @param field 字段名
     * @return 固定列表头
     */
    public static PivotTableHead fixed(String name, String field) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .columnType(ColumnType.FIXED)
                .build();
    }

    /**
     * 创建固定列（带宽度）
     *
     * @param name 表头名称
     * @param field 字段名
     * @param width 列宽
     * @return 固定列表头
     */
    public static PivotTableHead fixed(String name, String field, Integer width) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .width(width)
                .columnType(ColumnType.FIXED)
                .build();
    }

    /**
     * 创建动态列
     * 
     * <p>动态列显示在固定列和汇总列之间，数量可变。</p>
     *
     * @param name 表头名称
     * @param field 字段名
     * @return 动态列表头
     */
    public static PivotTableHead dynamic(String name, String field) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .columnType(ColumnType.DYNAMIC)
                .build();
    }

    /**
     * 创建动态列（带宽度）
     *
     * @param name 表头名称
     * @param field 字段名
     * @param width 列宽
     * @return 动态列表头
     */
    public static PivotTableHead dynamic(String name, String field, Integer width) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .width(width)
                .columnType(ColumnType.DYNAMIC)
                .build();
    }

    /**
     * 创建汇总列
     * 
     * <p>汇总列显示在表头最右侧。</p>
     *
     * @param name 表头名称
     * @param field 字段名
     * @return 汇总列表头
     */
    public static PivotTableHead summary(String name, String field) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .columnType(ColumnType.SUMMARY)
                .build();
    }

    /**
     * 创建汇总列（带宽度）
     *
     * @param name 表头名称
     * @param field 字段名
     * @param width 列宽
     * @return 汇总列表头
     */
    public static PivotTableHead summary(String name, String field, Integer width) {
        return PivotTableHead.builder()
                .name(name)
                .field(field)
                .width(width)
                .columnType(ColumnType.SUMMARY)
                .build();
    }

    /**
     * 转换为DynamicHead
     *
     * @return DynamicHead对象
     */
    public DynamicHead toDynamicHead() {
        return DynamicHead.builder()
                .name(this.name)
                .field(this.field)
                .width(this.width)
                .build();
    }
}
