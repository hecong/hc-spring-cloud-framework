package com.hc.framework.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hc.framework.excel.executor.ExcelAsyncExecutor;
import com.hc.framework.excel.listener.ImportDataListener;
import com.hc.framework.excel.model.ExcelImportRequest;
import com.hc.framework.excel.model.ExcelImportResult;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.model.multisheet.MultiSheetContext;
import com.hc.framework.excel.model.multisheet.MultiSheetImportBuilder;
import com.hc.framework.excel.model.multisheet.MultiSheetImportResult;
import com.hc.framework.excel.model.multisheet.SheetConfig;
import com.hc.framework.excel.model.multisheet.SheetError;
import com.hc.framework.excel.model.multisheet.SheetImportResult;
import com.hc.framework.excel.model.multisheet.ValidationResult;
import com.hc.framework.excel.service.ExcelImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel导入服务实现
 */
public class ExcelImportServiceImpl implements ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportServiceImpl.class);

    private final ExcelAsyncExecutor asyncExecutor;
    private final ObjectMapper objectMapper;

    public ExcelImportServiceImpl(ExcelAsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <T> ExcelImportResult<T> importData(InputStream inputStream, Class<T> clazz) {
        ImportDataListener<T> listener = new ImportDataListener<>();
        EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
        return listener.getResult();
    }

    @Override
    public <T> ExcelImportResult<T> importData(ExcelImportRequest request, Class<T> clazz) {
        ImportDataListener<T> listener = new ImportDataListener<>();
        Integer headRowNumber = request.getHeadRowNumber() != null ? request.getHeadRowNumber() : 0;

        try {
            EasyExcel.read(request.getFile().getInputStream(), clazz, listener)
                .sheet(request.getSheetName())
                .headRowNumber(headRowNumber)
                .registerReadListener(listener)
                .doRead();
        } catch (Exception e) {
            throw new RuntimeException("Excel导入失败", e);
        }

        return listener.getResult();
    }

    @Override
    public <T> void importData(InputStream inputStream, Class<T> clazz, ReadListener<T> readListener) {
        EasyExcel.read(inputStream, clazz, readListener).sheet().doRead();
    }

    @Override
    public <T> void importData(InputStream inputStream, Class<T> clazz, int batchSize, Consumer<List<T>> batchHandler) {
        ImportDataListener<T> listener = new ImportDataListener<>(
            batchSize, batchHandler, null, null, null);
        EasyExcel.read(inputStream, clazz, listener).sheet().doRead();
    }

    @Override
    public <T> String importDataAsync(ExcelImportRequest request, Class<T> clazz,
                                      Consumer<List<T>> batchHandler,
                                      Consumer<Integer> progressCallback) {
        return importDataAsync(request, clazz, batchHandler, null, progressCallback);
    }

    @Override
    public <T> String importDataAsync(ExcelImportRequest request, Class<T> clazz,
                                      Consumer<List<T>> batchHandler,
                                      Consumer<ExcelImportResult.ErrorRow<T>> errorHandler,
                                      Consumer<Integer> progressCallback) {
        String taskId = asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.IMPORT);
        int batchSize = request.getBatchSize() != null ? request.getBatchSize() : 1000;
        asyncExecutor.executeImport(taskId, request, clazz, batchSize, batchHandler, errorHandler, progressCallback);

        return taskId;
    }

    @Override
    public ExcelTaskStatus getTaskStatus(String taskId) {
        return asyncExecutor.getTaskStatus(taskId);
    }

    @Override
    public MultiSheetImportBuilder importMultiSheet(InputStream inputStream) {
        return new MultiSheetImportBuilder(inputStream, this);
    }

    @Override
    public MultiSheetImportResult importMultiSheet(InputStream inputStream, MultiSheetImportBuilder builder) {
        MultiSheetImportResult result = new MultiSheetImportResult();
        MultiSheetContext context = builder.getContext();

        try (ExcelReader excelReader = EasyExcel.read(inputStream).build()) {
            List<SheetConfig<?>> sheetConfigs = builder.getSheetConfigs();

            for (SheetConfig<?> config : sheetConfigs) {
                SheetImportResult<?> sheetResult = readSingleSheet(excelReader, config, context);
                result.addSheetResult(config.getSheetName(), sheetResult);

                // 缓存Sheet数据到上下文，供后续Sheet使用
                context.cacheSheetData(config.getSheetName(), sheetResult.getDataList());
            }
        } catch (Exception e) {
            log.error("多Sheet导入失败", e);
            throw new RuntimeException("多Sheet导入失败: " + e.getMessage(), e);
        }

        result.calculateSummary();
        return result;
    }

    /**
     * 读取单个Sheet
     *
     * @param excelReader Excel读取器
     * @param config      Sheet配置
     * @param context     多Sheet上下文
     * @param <T>         数据类型
     * @return Sheet导入结果
     */
    private <T> SheetImportResult<T> readSingleSheet(ExcelReader excelReader, SheetConfig<T> config, MultiSheetContext context) {
        SheetImportResult<T> result = new SheetImportResult<>();
        result.setSheetIndex(config.getSheetIndex());
        result.setSheetName(config.getSheetName());

        List<T> dataList = new ArrayList<>();
        List<SheetError> errorList = new ArrayList<>();

        try {
            ReadSheet readSheet;
            if (config.getSheetIndex() >= 0) {
                readSheet = EasyExcel.readSheet(config.getSheetIndex())
                    .head(config.getClazz())
                    .headRowNumber(config.getHeadRowNumber())
                    .registerReadListener(createReadListener(config, dataList, errorList, context))
                    .build();
            } else {
                readSheet = EasyExcel.readSheet(config.getSheetName())
                    .head(config.getClazz())
                    .headRowNumber(config.getHeadRowNumber())
                    .registerReadListener(createReadListener(config, dataList, errorList, context))
                    .build();
            }

            excelReader.read(readSheet);

        } catch (Exception e) {
            log.error("Sheet[{}]读取失败", config.getSheetName(), e);
            errorList.add(SheetError.of(config.getSheetIndex(), config.getSheetName(), 0, "Sheet读取失败: " + e.getMessage()));
        }

        // 填充结果
        result.setDataList(dataList);
        result.setErrors(errorList);
        result.setSuccessRows(dataList.size());
        result.setFailRows(errorList.size());
        result.setSuccess(errorList.isEmpty());
        result.calculateTotalRows();

        return result;
    }

    /**
     * 创建读取监听器
     *
     * @param config    Sheet配置
     * @param dataList  数据列表
     * @param errorList 错误列表
     * @param context   多Sheet上下文
     * @param <T>       数据类型
     * @return 读取监听器
     */
    private <T> ReadListener<T> createReadListener(SheetConfig<T> config, List<T> dataList,
                                                   List<SheetError> errorList, MultiSheetContext context) {
        return new ReadListener<T>() {
            private final List<T> batchCache = new ArrayList<>();

            @Override
            public void invoke(T data, AnalysisContext analysisContext) {
                int rowNum = analysisContext.readRowHolder().getRowIndex() + 1;

                try {
                    // 数据验证
                    ValidationResult<T> validationResult = validateData(data, config, context);

                    if (validationResult.isValid()) {
                        T validData = validationResult.getData() != null ? validationResult.getData() : data;
                        dataList.add(validData);
                        batchCache.add(validData);

                        // 达到批次大小时执行批次处理
                        if (batchCache.size() >= config.getBatchSize() && config.getBatchHandler() != null) {
                            config.getBatchHandler().accept(new ArrayList<>(batchCache));
                            batchCache.clear();
                        }
                    } else {
                        SheetError error = SheetError.of(
                            config.getSheetIndex(),
                            config.getSheetName(),
                            rowNum,
                            validationResult.getErrorMsg()
                        );
                        error.setDataJson(toJsonString(data));
                        errorList.add(error);

                        // 调用错误处理器
                        if (config.getErrorHandler() != null) {
                            config.getErrorHandler().accept(error);
                        }
                    }
                } catch (Exception e) {
                    log.error("第{}行数据处理异常", rowNum, e);
                    SheetError error = SheetError.of(
                        config.getSheetIndex(),
                        config.getSheetName(),
                        rowNum,
                        "处理异常: " + e.getMessage()
                    );
                    error.setDataJson(toJsonString(data));
                    errorList.add(error);

                    if (config.getErrorHandler() != null) {
                        config.getErrorHandler().accept(error);
                    }
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                // 处理剩余批次数据
                if (!batchCache.isEmpty() && config.getBatchHandler() != null) {
                    config.getBatchHandler().accept(new ArrayList<>(batchCache));
                    batchCache.clear();
                }
            }
        };
    }

    /**
     * 验证数据
     *
     * @param data    数据
     * @param config  Sheet配置
     * @param context 多Sheet上下文
     * @param <T>     数据类型
     * @return 验证结果
     */
    private <T> ValidationResult<T> validateData(T data, SheetConfig<T> config, MultiSheetContext context) {
        // 优先使用带上下文的验证器
        if (config.getValidatorWithContext() != null) {
            return config.getValidatorWithContext().apply(data, context);
        }

        // 使用普通验证器
        if (config.getValidator() != null) {
            return config.getValidator().apply(data);
        }

        // 无验证器，默认通过
        return ValidationResult.ok(data);
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param data 数据对象
     * @return JSON 字符串，转换失败时返回空对象字符串
     */
    private String toJsonString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("数据序列化为JSON失败", e);
            return "{}";
        }
    }
}
