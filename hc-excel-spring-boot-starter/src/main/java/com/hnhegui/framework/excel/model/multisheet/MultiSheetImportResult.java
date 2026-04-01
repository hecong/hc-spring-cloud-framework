package com.hnhegui.framework.excel.model.multisheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多Sheet导入汇总结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiSheetImportResult {

    /**
     * 是否全部成功
     */
    private boolean allSuccess;

    /**
     * 总行数（所有Sheet）
     */
    private int totalRows;

    /**
     * 总成功行数
     */
    private int totalSuccessRows;

    /**
     * 总失败行数
     */
    private int totalFailRows;

    /**
     * 各Sheet结果映射（key: sheetName, value: SheetImportResult）
     */
    @Builder.Default
    private Map<String, SheetImportResult<?>> sheetResults = new HashMap<>();

    /**
     * 所有错误列表
     */
    @Builder.Default
    private List<SheetError> allErrors = new ArrayList<>();

    /**
     * 添加Sheet结果
     *
     * @param sheetName Sheet名称
     * @param result    Sheet导入结果
     */
    public void addSheetResult(String sheetName, SheetImportResult<?> result) {
        sheetResults.put(sheetName, result);
        totalSuccessRows += result.getSuccessRows();
        totalFailRows += result.getFailRows();
        allErrors.addAll(result.getErrors());
    }

    /**
     * 计算汇总结果
     */
    public void calculateSummary() {
        this.totalRows = totalSuccessRows + totalFailRows;
        this.allSuccess = allErrors.isEmpty();
    }

    /**
     * 获取指定Sheet的结果
     *
     * @param sheetName Sheet名称
     * @param <T>       数据类型
     * @return Sheet导入结果
     */
    @SuppressWarnings("unchecked")
    public <T> SheetImportResult<T> getSheetResult(String sheetName) {
        return (SheetImportResult<T>) sheetResults.get(sheetName);
    }

    /**
     * 获取错误信息列表
     *
     * @return 错误信息列表
     */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<>();
        for (SheetError error : allErrors) {
            messages.add(error.toString());
        }
        return messages;
    }

    /**
     * 获取指定Sheet的错误列表
     *
     * @param sheetName Sheet名称
     * @return 错误列表
     */
    public List<SheetError> getSheetErrors(String sheetName) {
        SheetImportResult<?> result = sheetResults.get(sheetName);
        return result != null ? result.getErrors() : new ArrayList<>();
    }

    /**
     * 获取指定Sheet的错误信息列表
     *
     * @param sheetName Sheet名称
     * @return 错误信息列表
     */
    public List<String> getSheetErrorMessages(String sheetName) {
        SheetImportResult<?> result = sheetResults.get(sheetName);
        return result != null ? result.getErrorMessages() : new ArrayList<>();
    }

    /**
     * 获取指定Sheet的成功数据列表
     *
     * @param sheetName Sheet名称
     * @param <T>       数据类型
     * @return 数据列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getSheetDataList(String sheetName) {
        SheetImportResult<T> result = (SheetImportResult<T>) sheetResults.get(sheetName);
        return result != null ? result.getDataList() : new ArrayList<>();
    }
}
