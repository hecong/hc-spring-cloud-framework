package com.hc.framework.excel.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel导入结果
 *
 * @param <T> 数据类型
 */
@Data
@Builder
public class ExcelImportResult<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 总行数
     */
    private int totalRows;

    /**
     * 成功行数
     */
    private int successRows;

    /**
     * 失败行数
     */
    private int failRows;

    /**
     * 成功的数据列表
     */
    @Builder.Default
    private List<T> dataList = new ArrayList<>();

    /**
     * 失败详情
     */
    @Builder.Default
    private List<ErrorRow<T>> errorRows = new ArrayList<>();

    /**
     * 错误行信息
     */
    @Data
    @Builder
    public static class ErrorRow<T> {
        /**
         * 行号
         */
        private int rowNum;
        /**
         * 行数据
         */
        private T data;
        /**
         * 错误信息
         */
        private String errorMsg;
    }

    /**
     * 创建成功结果
     */
    public static <T> ExcelImportResult<T> success(int totalRows, List<T> dataList) {
        return ExcelImportResult.<T>builder()
                .success(true)
                .totalRows(totalRows)
                .successRows(dataList.size())
                .failRows(0)
                .dataList(dataList)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static <T> ExcelImportResult<T> fail(int totalRows, int successRows, List<ErrorRow<T>> errorRows) {
        return ExcelImportResult.<T>builder()
                .success(successRows > 0 && errorRows.isEmpty())
                .totalRows(totalRows)
                .successRows(successRows)
                .failRows(errorRows.size())
                .errorRows(errorRows)
                .build();
    }
}
