package com.hnhegui.framework.excel.listener;

import com.hnhegui.framework.excel.model.ExcelImportResult;
import com.hnhegui.framework.excel.model.ExcelTaskStatus;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel导入数据监听器
 * 支持分批处理、错误收集、进度回调
 *
 * @param <T> 数据类型
 */
public class ImportDataListener<T> implements ReadListener<T> {

    private static final Logger log = LoggerFactory.getLogger(ImportDataListener.class);

    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 批次大小
     */
    private final int batchSize;

    /**
     * 当前批次数据
     */
    private final List<T> currentBatch = new ArrayList<>();

    /**
     * 批处理回调
     */
    private Consumer<List<T>> batchHandler;

    /**
     * 错误处理回调
     */
    private Consumer<ExcelImportResult.ErrorRow<T>> errorHandler;

    /**
     * 任务状态
     */
    private ExcelTaskStatus taskStatus;

    /**
     * 进度回调
     */
    private Consumer<Integer> progressCallback;

    /**
     * 总行数
     */
    private int totalRows;

    /**
     * 成功行数
     */
    private int successRows;

    /**
     * 失败详情列表
     */
    private final List<ExcelImportResult.ErrorRow<T>> errorRows = new ArrayList<>();

    /**
     * 成功的数据列表（同步模式使用）
     */
    private final List<T> successDataList = new ArrayList<>();

    /**
     * 当前行号
     */
    private int currentRowNum;

    /**
     * 构造函数 - 同步导入用
     */
    public ImportDataListener() {
        this.batchSize = DEFAULT_BATCH_SIZE;
    }

    /**
     * 构造函数 - 异步导入用
     *
     * @param batchSize       批次大小
     * @param batchHandler    批处理回调
     * @param errorHandler    错误处理回调
     * @param taskStatus      任务状态
     * @param progressCallback 进度回调
     */
    public ImportDataListener(int batchSize,
                              Consumer<List<T>> batchHandler,
                              Consumer<ExcelImportResult.ErrorRow<T>> errorHandler,
                              ExcelTaskStatus taskStatus,
                              Consumer<Integer> progressCallback) {
        this.batchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        this.batchHandler = batchHandler;
        this.errorHandler = errorHandler;
        this.taskStatus = taskStatus;
        this.progressCallback = progressCallback;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        currentRowNum = context.readRowHolder().getRowIndex() + 1;
        totalRows++;
        currentBatch.add(data);

        // 达到批次大小时处理
        if (currentBatch.size() >= batchSize) {
            processBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余数据
        if (!currentBatch.isEmpty()) {
            processBatch();
        }
        log.info("Excel解析完成, 总行数: {}, 成功: {}, 失败: {}", totalRows, successRows, errorRows.size());
    }

    /**
     * 处理当前批次数据
     */
    private void processBatch() {
        if (currentBatch.isEmpty()) {
            return;
        }

        List<T> batchData = new ArrayList<>(currentBatch);
        currentBatch.clear();

        try {
            if (batchHandler != null) {
                // 异步模式：使用回调处理
                batchHandler.accept(batchData);
                successRows += batchData.size();
            } else {
                // 同步模式：收集数据到 successDataList
                successDataList.addAll(batchData);
                successRows += batchData.size();
            }

            // 更新任务状态
            if (taskStatus != null) {
                for (int i = 0; i < batchData.size(); i++) {
                    taskStatus.incrementSuccess();
                }
                if (progressCallback != null) {
                    progressCallback.accept(taskStatus.getProgress());
                }
            }

        } catch (Exception e) {
            log.error("批次处理失败, batchSize: {}", batchData.size(), e);
            // 记录整批失败
            for (T data : batchData) {
                ExcelImportResult.ErrorRow<T> errorRow = ExcelImportResult.ErrorRow.<T>builder()
                        .rowNum(currentRowNum - currentBatch.size() + batchData.indexOf(data))
                        .data(data)
                        .errorMsg(e.getMessage())
                        .build();
                errorRows.add(errorRow);

                if (errorHandler != null) {
                    errorHandler.accept(errorRow);
                }

                if (taskStatus != null) {
                    taskStatus.incrementFail();
                }
            }
        }
    }

    /**
     * 获取导入结果（同步模式使用）
     */
    public ExcelImportResult<T> getResult() {
        return ExcelImportResult.<T>builder()
                .success(errorRows.isEmpty())
                .totalRows(totalRows)
                .successRows(successRows)
                .failRows(errorRows.size())
                .dataList(successDataList)
                .errorRows(errorRows)
                .build();
    }

    /**
     * 添加错误行（供外部调用）
     */
    public void addErrorRow(int rowNum, T data, String errorMsg) {
        ExcelImportResult.ErrorRow<T> errorRow = ExcelImportResult.ErrorRow.<T>builder()
                .rowNum(rowNum)
                .data(data)
                .errorMsg(errorMsg)
                .build();
        errorRows.add(errorRow);
        totalRows++;
        if (taskStatus != null) {
            taskStatus.incrementFail();
        }
    }
}
