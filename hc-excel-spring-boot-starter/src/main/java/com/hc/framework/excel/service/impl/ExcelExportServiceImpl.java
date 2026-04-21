package com.hc.framework.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.hc.framework.excel.executor.ExcelAsyncExecutor;
import com.hc.framework.excel.model.DynamicHead;
import com.hc.framework.excel.model.ExcelExportRequest;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.model.TemplateExportRequest;
import com.hc.framework.excel.service.ExcelExportService;
import com.hc.framework.excel.util.ExcelHeadUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Excel导出服务实现
 */
public class ExcelExportServiceImpl implements ExcelExportService {

    private final ExcelAsyncExecutor asyncExecutor;

    public ExcelExportServiceImpl(ExcelAsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    // ==================== 基础导出 ====================

    @Override
    public <T> void exportData(ExcelExportRequest request, List<T> data, Class<T> clazz, OutputStream outputStream) {
        Integer headRowNumber = request.getHeadRowNumber() != null ? request.getHeadRowNumber() : 0;
        String sheetName = request.getSheetName() != null ? request.getSheetName() : "Sheet1";

        EasyExcel.write(outputStream, clazz)
                .sheet(headRowNumber, sheetName)
                .doWrite(data);
    }

    @Override
    public <T> void exportData(ExcelExportRequest request, Supplier<List<T>> dataQuery, Class<T> clazz, OutputStream outputStream) {
        int batchSize = request.getBatchSize() != null ? request.getBatchSize() : 5000;
        Integer headRowNumber = request.getHeadRowNumber() != null ? request.getHeadRowNumber() : 0;
        String sheetName = request.getSheetName() != null ? request.getSheetName() : "Sheet1";

        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, clazz).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet(headRowNumber, sheetName).build();

            List<T> data;
            do {
                data = dataQuery.get();
                if (data != null && !data.isEmpty()) {
                    excelWriter.write(data, writeSheet);
                }
            } while (data != null && data.size() >= batchSize);
        }
    }

    @Override
    public <T> String exportDataAsync(ExcelExportRequest request, Supplier<List<T>> dataQuery,
                                       Class<T> clazz, Consumer<ExcelTaskStatus> progressCallback) {
        String taskId = asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        // 记录导出操作（无感知，异步获取数据条数会影响性能，记录基本信息）
        asyncExecutor.executeExport(taskId, request, dataQuery, clazz, progressCallback);
        return taskId;
    }

    @Override
    public <T> String exportDataAsyncAll(ExcelExportRequest request, Supplier<List<T>> allDataQuery,
                                         Class<T> clazz, Consumer<ExcelTaskStatus> progressCallback) {
        String taskId = asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        // 记录导出操作
        asyncExecutor.executeExportAll(taskId, request, allDataQuery, clazz, progressCallback);
        return taskId;
    }

    // ==================== 模板导出 ====================

    @Override
    public void exportByTemplate(TemplateExportRequest request, OutputStream outputStream) {
        try (InputStream templateStream = getTemplateStream(request);
             ExcelWriter excelWriter = EasyExcel.write(outputStream)
                     .withTemplate(templateStream)
                     .build()) {
            WriteSheet writeSheet = getWriteSheet(request);

            if (request.getData() != null) {
                excelWriter.fill(request.getData(), writeSheet);
            }
        } catch (Exception e) {
            throw new RuntimeException("模板导出失败", e);
        }
    }

    @Override
    public <T> void exportByTemplate(TemplateExportRequest request, List<T> dataList, OutputStream outputStream) {
        try (InputStream templateStream = getTemplateStream(request);
             ExcelWriter excelWriter = EasyExcel.write(outputStream)
                     .withTemplate(templateStream)
                     .build()) {
            WriteSheet writeSheet = getWriteSheet(request);

            if (request.getData() != null) {
                excelWriter.fill(request.getData(), writeSheet);
            }

            if (dataList != null && !dataList.isEmpty()) {
                FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
                excelWriter.fill(dataList, fillConfig, writeSheet);
            }
        } catch (Exception e) {
            throw new RuntimeException("模板导出失败", e);
        }
    }

    @Override
    public <T> String exportByTemplateAsync(TemplateExportRequest request, Supplier<List<T>> dataQuery,
                                             Consumer<ExcelTaskStatus> progressCallback) {
        String taskId = asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        asyncExecutor.executeTemplateExport(taskId, request, dataQuery, progressCallback);
        return taskId;
    }

    // ==================== 动态表头导出 ====================

    @Override
    public void exportWithDynamicHead(List<DynamicHead> heads, List<Map<String, Object>> dataList, OutputStream outputStream) {
        List<List<String>> headList = ExcelHeadUtil.buildDynamicHeads(heads);

        EasyExcel.write(outputStream)
                .head(headList)
                .sheet("Sheet1")
                .doWrite(() -> ExcelHeadUtil.buildDynamicRows(heads, dataList));
    }

    @Override
    public void exportWithDynamicHead(List<DynamicHead> heads, Supplier<List<Map<String, Object>>> dataQuery, OutputStream outputStream) {
        List<List<String>> headList = ExcelHeadUtil.buildDynamicHeads(heads);

        try (ExcelWriter excelWriter = EasyExcel.write(outputStream).head(headList).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();

            List<Map<String, Object>> data;
            while (true) {
                data = dataQuery.get();
                if (data == null || data.isEmpty()) {
                    break;
                }
                List<List<Object>> rows = ExcelHeadUtil.buildDynamicRows(heads, data);
                excelWriter.write(rows, writeSheet);
            }
        }
    }

    @Override
    public String exportWithDynamicHeadAsync(String fileName, String sheetName, List<DynamicHead> heads,
                                              Supplier<List<Map<String, Object>>> dataQuery, Consumer<ExcelTaskStatus> progressCallback) {
        String taskId = asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        asyncExecutor.executeDynamicHeadExport(taskId, sheetName, heads, dataQuery, progressCallback);
        return taskId;
    }

    // ==================== 任务管理 ====================

    @Override
    public ExcelTaskStatus getTaskStatus(String taskId) {
        return asyncExecutor.getTaskStatus(taskId);
    }

    @Override
    public String getExportFilePath(String taskId) {
        return asyncExecutor.getExportFilePath(taskId);
    }

    // ==================== 私有方法 ====================

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
        Integer sheetIndex = request.getSheetIndex() != null ? request.getSheetIndex() : 0;
        return EasyExcel.writerSheet(sheetIndex).build();
    }

}
