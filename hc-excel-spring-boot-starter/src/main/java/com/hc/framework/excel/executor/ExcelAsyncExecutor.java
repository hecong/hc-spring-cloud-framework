package com.hc.framework.excel.executor;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.hc.framework.excel.listener.ImportDataListener;
import com.hc.framework.excel.model.DynamicHead;
import com.hc.framework.excel.model.ExcelExportRequest;
import com.hc.framework.excel.model.ExcelImportRequest;
import com.hc.framework.excel.model.ExcelImportResult;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.model.TemplateExportRequest;
import com.hc.framework.excel.service.ExcelFileStorage;
import com.hc.framework.excel.service.ExcelOperationRecorder;
import com.hc.framework.excel.service.ExcelOperatorResolver;
import com.hc.framework.excel.util.ExcelHeadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Excel异步执行器
 *
 * <p>独立组件，解决同类调用@Async不生效问题。</p>
 * <p>提供Excel导入、导出、模板导出、动态表头导出的异步执行能力。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class ExcelAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(ExcelAsyncExecutor.class);

    private final ExcelOperationRecorder operationRecorder;
    private final ExcelFileStorage fileStorage;
    private final ExcelOperatorResolver operatorResolver;

    public ExcelAsyncExecutor(ExcelOperationRecorder operationRecorder,
                              ExcelFileStorage fileStorage, ExcelOperatorResolver operatorResolver) {
        this.operationRecorder = operationRecorder;
        this.fileStorage = fileStorage;
        this.operatorResolver = operatorResolver;

    }

    // ==================== 常量定义 ====================

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final int DEFAULT_HEAD_ROW_NUMBER = 0;
    private static final String DEFAULT_SHEET_NAME = "Sheet1";
    private static final String DEFAULT_FILE_NAME = "export";

    // ==================== 任务存储 ====================

    private final ConcurrentHashMap<String, ExcelTaskStatus> taskStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> exportFileStore = new ConcurrentHashMap<>();

    // ==================== 任务管理 ====================

    /**
     * 创建任务ID
     *
     * @param type 任务类型
     * @return 任务ID
     */
    public String createTaskId(ExcelTaskStatus.TaskType type) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskStore.put(taskId, new ExcelTaskStatus(taskId, type));
        return taskId;
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    public ExcelTaskStatus getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    /**
     * 获取导出文件URL（云端地址）
     *
     * @param taskId 任务ID
     * @return 文件URL
     */
    public String getExportFilePath(String taskId) {
        return exportFileStore.get(taskId);
    }

    /**
     * 移除任务（清理资源）
     *
     * @param taskId 任务ID
     */
    public void removeTask(String taskId) {
        String fileUrl = exportFileStore.remove(taskId);
        if (fileUrl != null) {
            // 删除云端文件
            fileStorage.delete(fileUrl);
        }
        taskStore.remove(taskId);
    }

    // ==================== 数据导入 ====================

    /**
     * 异步执行导入任务
     *
     * @param taskId           任务ID
     * @param request          导入请求
     * @param clazz            数据类型
     * @param batchSize        批次大小
     * @param batchHandler     批次处理器
     * @param errorHandler     错误处理器
     * @param progressCallback 进度回调
     */
    @Async("excelAsyncExecutor")
    public <T> void executeImport(String taskId, ExcelImportRequest request, Class<T> clazz,
                                  int batchSize, Consumer<List<T>> batchHandler,
                                  Consumer<ExcelImportResult.ErrorRow<T>> errorHandler,
                                  Consumer<Integer> progressCallback) {
        ExcelTaskStatus taskStatus = taskStore.get(taskId);
        String operatorName = operatorResolver.getOperatorName();
        String operatorId = operatorResolver.getOperatorId();

        try {
            ImportDataListener<T> listener = new ImportDataListener<>(
                batchSize, batchHandler, errorHandler, taskStatus, progressCallback);

            EasyExcel.read(request.getFile().getInputStream(), clazz, listener)
                .sheet(request.getSheetName())
                .headRowNumber(getHeadRowNumber(request.getHeadRowNumber()))
                .registerReadListener(listener)
                .doRead();

            completeTask(taskId, taskStatus,
                taskStatus.getSuccessCount().get(), taskStatus.getFailCount().get());

            // 记录导入完成
            operationRecorder.recordImportComplete(taskId, operatorId, operatorName, true,
                taskStatus.getSuccessCount().get(),
                taskStatus.getFailCount().get(),
                null);

        } catch (Exception e) {
            failTask(taskId, taskStatus, "导入", e);
            // 记录导入失败
            operationRecorder.recordImportComplete(taskId, operatorId, operatorName, false, 0, 0, e.getMessage());
        }
    }

    // ==================== 数据导出 ====================

    /**
     * 异步执行导出任务（分批查询）
     *
     * @param taskId           任务ID
     * @param request          导出请求
     * @param dataQuery        数据查询（分批）
     * @param clazz            数据类型
     * @param progressCallback 进度回调
     */
    @Async("excelAsyncExecutor")
    public <T> void executeExport(String taskId, ExcelExportRequest request,
                                  Supplier<List<T>> dataQuery, Class<T> clazz,
                                  Consumer<Integer> progressCallback) {
        ExcelTaskStatus taskStatus = taskStore.get(taskId);
        String tempFilePath = buildTempFilePath(request.getFileName(), taskId);
        int batchSize = getBatchSize(request.getBatchSize());
        String operatorId = operatorResolver.getOperatorId();
        String operatorName = operatorResolver.getOperatorName();

        try {
            File tempFile = new File(tempFilePath);
            int totalCount = 0;

            try (ExcelWriter excelWriter = EasyExcel.write(tempFile, clazz).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet(
                    getHeadRowNumber(request.getHeadRowNumber()),
                    getSheetName(request.getSheetName())).build();

                List<T> data;
                do {
                    data = dataQuery.get();
                    if (data != null && !data.isEmpty()) {
                        excelWriter.write(data, writeSheet);
                        totalCount += data.size();
                        updateProgress(taskStatus, progressCallback, totalCount);
                    }
                } while (data != null && data.size() >= batchSize);
            }

            // 上传文件到云端存储
            String fileUrl = fileStorage.upload(tempFile, taskId);

            completeExportTask(taskId, taskStatus, fileUrl, totalCount, "导出");

            // 记录导出完成（使用云端URL）
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, true, fileUrl, null);

        } catch (Exception e) {
            failTask(taskId, taskStatus, "导出", e);
            // 记录导出失败
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, false, null, e.getMessage());
        }
    }

    /**
     * 异步执行导出任务（全量数据）
     *
     * @param taskId           任务ID
     * @param request          导出请求
     * @param allDataQuery     数据查询（全量）
     * @param clazz            数据类型
     * @param progressCallback 进度回调
     */
    @Async("excelAsyncExecutor")
    public <T> void executeExportAll(String taskId, ExcelExportRequest request,
                                     Supplier<List<T>> allDataQuery, Class<T> clazz,
                                     Consumer<Integer> progressCallback) {
        ExcelTaskStatus taskStatus = taskStore.get(taskId);
        String tempFilePath = buildTempFilePath(request.getFileName(), taskId);
        String operatorId = operatorResolver.getOperatorId();
        String operatorName = operatorResolver.getOperatorName();

        try {
            File tempFile = new File(tempFilePath);
            List<T> allData = allDataQuery.get();
            int totalCount = (allData != null) ? allData.size() : 0;

            if (allData != null && !allData.isEmpty()) {
                taskStatus.setTotal(totalCount);
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    EasyExcel.write(outputStream, clazz)
                        .sheet(getHeadRowNumber(request.getHeadRowNumber()),
                            getSheetName(request.getSheetName()))
                        .doWrite(allData);
                }
                taskStatus.setProcessed(new AtomicInteger(totalCount));
            }

            // 上传文件到云端存储
            String fileUrl = fileStorage.upload(tempFile, taskId);

            completeExportTask(taskId, taskStatus, fileUrl, totalCount, "导出", progressCallback);

            // 记录导出完成（使用云端URL）
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, true, fileUrl, null);

        } catch (Exception e) {
            failTask(taskId, taskStatus, "导出", e);
            // 记录导出失败
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, false, null, e.getMessage());
        }
    }

    // ==================== 模板导出 ====================

    /**
     * 异步执行模板导出任务
     *
     * @param taskId           任务ID
     * @param request          模板导出请求
     * @param dataQuery        数据查询
     * @param progressCallback 进度回调
     */
    @Async("excelAsyncExecutor")
    public <T> void executeTemplateExport(String taskId, TemplateExportRequest request,
                                          Supplier<List<T>> dataQuery,
                                          Consumer<Integer> progressCallback) {
        ExcelTaskStatus taskStatus = taskStore.get(taskId);
        String tempFilePath = buildTempFilePath(request.getFileName(), taskId);
        String operatorId = operatorResolver.getOperatorId();
        String operatorName = operatorResolver.getOperatorName();
        try {
            File tempFile = new File(tempFilePath);
            int totalCount = 0;

            try (InputStream templateStream = getTemplateStream(request);
                 OutputStream outputStream = new FileOutputStream(tempFile);
                 ExcelWriter excelWriter = EasyExcel.write(outputStream)
                     .withTemplate(templateStream)
                     .build()) {

                WriteSheet writeSheet = getWriteSheet(request);

                if (request.getData() != null) {
                    excelWriter.fill(request.getData(), writeSheet);
                }

                FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
                List<T> data;
                while (true) {
                    data = dataQuery.get();
                    if (data == null || data.isEmpty()) {
                        break;
                    }
                    excelWriter.fill(data, fillConfig, writeSheet);
                    totalCount += data.size();
                    updateProgress(taskStatus, progressCallback, totalCount);
                }
            }

            // 上传文件到云端存储
            String fileUrl = fileStorage.upload(tempFile, taskId);

            completeExportTask(taskId, taskStatus, fileUrl, totalCount, "模板导出");

            // 记录模板导出完成（使用云端URL）
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, true, fileUrl, null);

        } catch (Exception e) {
            failTask(taskId, taskStatus, "模板导出", e);
            // 记录模板导出失败
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, false, null, e.getMessage());
        }
    }

    // ==================== 动态表头导出 ====================

    /**
     * 异步执行动态表头导出任务
     *
     * @param taskId           任务ID
     * @param sheetName        Sheet名称
     * @param heads            动态表头定义
     * @param dataQuery        数据查询
     * @param progressCallback 进度回调
     */
    @Async("excelAsyncExecutor")
    public void executeDynamicHeadExport(String taskId, String sheetName,
                                         List<DynamicHead> heads,
                                         Supplier<List<Map<String, Object>>> dataQuery,
                                         Consumer<Integer> progressCallback) {
        ExcelTaskStatus taskStatus = taskStore.get(taskId);
        String tempFilePath = buildTempFilePath("export", taskId);
        String operatorId = operatorResolver.getOperatorId();
        String operatorName = operatorResolver.getOperatorName();
        try {
            File tempFile = new File(tempFilePath);
            List<List<String>> headList = ExcelHeadUtil.buildDynamicHeads(heads);
            int totalCount = 0;

            try (ExcelWriter excelWriter = EasyExcel.write(tempFile).head(headList).build()) {
                WriteSheet writeSheet = EasyExcel.writerSheet(getSheetName(sheetName)).build();

                List<Map<String, Object>> data;
                while (true) {
                    data = dataQuery.get();
                    if (data == null || data.isEmpty()) {
                        break;
                    }
                    excelWriter.write(ExcelHeadUtil.buildDynamicRows(heads, data), writeSheet);
                    totalCount += data.size();
                    updateProgress(taskStatus, progressCallback, totalCount);
                }
            }

            // 上传文件到云端存储
            String fileUrl = fileStorage.upload(tempFile, taskId);

            completeExportTask(taskId, taskStatus, fileUrl, totalCount, "动态表头导出");

            // 记录动态表头导出完成（使用云端URL）
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, true, fileUrl, null);

        } catch (Exception e) {
            failTask(taskId, taskStatus, "动态表头导出", e);
            // 记录动态表头导出失败
            operationRecorder.recordExportComplete(taskId, operatorId, operatorName, false, null, e.getMessage());
        }
    }

    // ==================== 私有方法 - 默认值获取 ====================

    private int getHeadRowNumber(Integer headRowNumber) {
        return headRowNumber != null ? headRowNumber : DEFAULT_HEAD_ROW_NUMBER;
    }

    private int getBatchSize(Integer batchSize) {
        return batchSize != null ? batchSize : DEFAULT_BATCH_SIZE;
    }

    private String getSheetName(String sheetName) {
        return sheetName != null ? sheetName : DEFAULT_SHEET_NAME;
    }

    private String getFileName(String fileName) {
        return fileName != null ? fileName : DEFAULT_FILE_NAME;
    }

    // ==================== 私有方法 - 路径与文件操作 ====================

    private String buildTempFilePath(String fileName, String taskId) {
        return TEMP_DIR + File.separator + getFileName(fileName) + "_" + taskId + ".xlsx";
    }

    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    // ==================== 私有方法 - 任务状态更新 ====================

    private void updateProgress(ExcelTaskStatus taskStatus, Consumer<Integer> progressCallback, int count) {
        taskStatus.setProcessed(new AtomicInteger(count));
        if (progressCallback != null) {
            progressCallback.accept(count);
        }
    }

    private void completeTask(String taskId, ExcelTaskStatus taskStatus, Object... logArgs) {
        taskStatus.complete();
        log.info("Excel{}任务完成, taskId: {}", "导入", appendTaskId(taskId, logArgs));
    }

    private void completeExportTask(String taskId, ExcelTaskStatus taskStatus,
                                    String fileUrl, int totalCount, String taskType,
                                    Consumer<Integer> progressCallback) {
        exportFileStore.put(taskId, fileUrl);
        taskStatus.setTotal(totalCount);
        taskStatus.complete();
        if (progressCallback != null) {
            progressCallback.accept(totalCount);
        }
        log.info("Excel{}任务完成, taskId: {}, 总行数: {}, 文件URL: {}", taskType, taskId, totalCount, fileUrl);
    }

    private void completeExportTask(String taskId, ExcelTaskStatus taskStatus,
                                    String filePath, int totalCount, String taskType) {
        completeExportTask(taskId, taskStatus, filePath, totalCount, taskType, null);
    }

    private void failTask(String taskId, ExcelTaskStatus taskStatus, String taskType, Exception e) {
        log.error("Excel{}任务失败, taskId: {}", taskType, taskId, e);
        taskStatus.fail(e.getMessage());
    }

    private Object[] appendTaskId(String taskId, Object... args) {
        Object[] result = new Object[args.length + 1];
        result[0] = taskId;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    // ==================== 私有方法 - 模板处理 ====================

    private InputStream getTemplateStream(TemplateExportRequest request) throws Exception {
        if (request.getTemplateStream() != null) {
            return request.getTemplateStream();
        }
        if (request.getTemplatePath() != null) {
            return new FileInputStream(request.getTemplatePath());
        }
        throw new IllegalArgumentException("模板流或模板路径必须提供其一");
    }

    private WriteSheet getWriteSheet(TemplateExportRequest request) {
        if (request.getSheetName() != null) {
            return EasyExcel.writerSheet(request.getSheetName()).build();
        }
        return EasyExcel.writerSheet(
            request.getSheetIndex() != null ? request.getSheetIndex() : DEFAULT_HEAD_ROW_NUMBER).build();
    }

}
