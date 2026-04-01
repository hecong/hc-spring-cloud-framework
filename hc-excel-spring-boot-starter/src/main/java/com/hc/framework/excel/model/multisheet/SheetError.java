package com.hc.framework.excel.model.multisheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sheet错误信息
 * 用于记录多Sheet导入中某一行的错误信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetError {

    /**
     * Sheet索引
     */
    private Integer sheetIndex;

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 行号（从1开始）
     */
    private Integer rowNum;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 原始数据（JSON字符串）
     */
    private String dataJson;

    /**
     * 快速创建错误信息
     *
     * @param sheetIndex Sheet索引
     * @param sheetName  Sheet名称
     * @param rowNum     行号
     * @param errorMsg   错误信息
     * @return SheetError
     */
    public static SheetError of(Integer sheetIndex, String sheetName, Integer rowNum, String errorMsg) {
        return SheetError.builder()
                .sheetIndex(sheetIndex)
                .sheetName(sheetName)
                .rowNum(rowNum)
                .errorMsg(errorMsg)
                .build();
    }

    @Override
    public String toString() {
        return String.format("[%s]第%d行: %s", sheetName, rowNum, errorMsg);
    }
}
