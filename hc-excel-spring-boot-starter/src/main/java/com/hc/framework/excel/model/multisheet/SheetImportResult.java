package com.hc.framework.excel.model.multisheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个Sheet导入结果
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetImportResult<T> {

    /**
     * Sheet索引
     */
    private Integer sheetIndex;

    /**
     * Sheet名称
     */
    private String sheetName;

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
     * 错误列表
     */
    @Builder.Default
    private List<SheetError> errors = new ArrayList<>();

    /**
     * 获取错误信息列表
     *
     * @return 错误信息列表
     */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<>();
        for (SheetError error : errors) {
            messages.add(error.toString());
        }
        return messages;
    }

    /**
     * 添加错误
     *
     * @param rowNum   行号
     * @param errorMsg 错误信息
     */
    public void addError(int rowNum, String errorMsg) {
        errors.add(SheetError.of(sheetIndex, sheetName, rowNum, errorMsg));
        failRows++;
    }

    /**
     * 添加成功数据
     *
     * @param data 数据
     */
    public void addSuccessData(T data) {
        dataList.add(data);
        successRows++;
    }

    /**
     * 计算总行数
     */
    public void calculateTotalRows() {
        this.totalRows = successRows + failRows;
    }
}
