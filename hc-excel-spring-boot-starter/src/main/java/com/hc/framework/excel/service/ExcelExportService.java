package com.hc.framework.excel.service;

import com.hc.framework.excel.model.DynamicHead;
import com.hc.framework.excel.model.ExcelExportRequest;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.model.TemplateExportRequest;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Excel导出服务接口
 */
public interface ExcelExportService {

    /**
     * 同步导出（小数据）
     *
     * @param request     导出请求
     * @param data        数据列表
     * @param clazz       数据模型类
     * @param outputStream 输出流
     * @param <T>         数据类型
     */
    <T> void exportData(ExcelExportRequest request, List<T> data, Class<T> clazz, OutputStream outputStream);

    /**
     * 同步导出-分页查询（小数据）
     *
     * @param request        导出请求
     * @param dataQuery       数据查询回调
     * @param clazz           数据模型类
     * @param outputStream    输出流
     * @param <T>             数据类型
     */
    <T> void exportData(ExcelExportRequest request, Supplier<List<T>> dataQuery, Class<T> clazz, OutputStream outputStream);

    /**
     * 异步导出-分页查询（大数据）
     *
     * @param request          导出请求
     * @param dataQuery        数据查询回调（分页）
     * @param clazz            数据模型类
     * @param progressCallback 进度回调
     * @param <T>              数据类型
     * @return 任务ID
     */
    <T> String exportDataAsync(ExcelExportRequest request, Supplier<List<T>> dataQuery,
                                Class<T> clazz, Consumer<ExcelTaskStatus> progressCallback);

    /**
     * 异步导出-全部数据查询（大数据）
     *
     * @param request          导出请求
     * @param allDataQuery      全部数据查询回调
     * @param clazz            数据模型类
     * @param progressCallback 进度回调
     * @param <T>              数据类型
     * @return 任务ID
     */
    <T> String exportDataAsyncAll(ExcelExportRequest request, Supplier<List<T>> allDataQuery,
                                  Class<T> clazz, Consumer<ExcelTaskStatus> progressCallback);

    // ==================== 模板导出 ====================

    /**
     * 模板导出-单行数据填充
     *
     * @param request      模板导出请求
     * @param outputStream 输出流
     */
    void exportByTemplate(TemplateExportRequest request, OutputStream outputStream);

    /**
     * 模板导出-列表数据填充
     *
     * @param request      模板导出请求
     * @param dataList     列表数据
     * @param outputStream 输出流
     * @param <T>          数据类型
     */
    <T> void exportByTemplate(TemplateExportRequest request, List<T> dataList, OutputStream outputStream);

    /**
     * 异步模板导出
     *
     * @param request          模板导出请求
     * @param dataQuery        数据查询回调
     * @param progressCallback 进度回调
     * @param <T>              数据类型
     * @return 任务ID
     */
    <T> String exportByTemplateAsync(TemplateExportRequest request, Supplier<List<T>> dataQuery,
                                      Consumer<ExcelTaskStatus> progressCallback);

    // ==================== 动态表头导出 ====================

    /**
     * 动态表头导出
     *
     * @param heads        动态表头列表
     * @param dataList     数据列表（Map格式）
     * @param outputStream 输出流
     */
    void exportWithDynamicHead(List<DynamicHead> heads, List<Map<String, Object>> dataList, OutputStream outputStream);

    /**
     * 动态表头导出-分页查询
     *
     * @param heads        动态表头列表
     * @param dataQuery    数据查询回调
     * @param outputStream 输出流
     */
    void exportWithDynamicHead(List<DynamicHead> heads, Supplier<List<Map<String, Object>>> dataQuery, OutputStream outputStream);

    /**
     * 异步动态表头导出
     *
     * @param fileName         文件名
     * @param sheetName        Sheet名称
     * @param heads            动态表头列表
     * @param dataQuery        数据查询回调
     * @param progressCallback 进度回调
     * @return 任务ID
     */
    String exportWithDynamicHeadAsync(String fileName, String sheetName, List<DynamicHead> heads,
                                       Supplier<List<Map<String, Object>>> dataQuery, Consumer<ExcelTaskStatus> progressCallback);

    // ==================== 任务管理 ====================

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    ExcelTaskStatus getTaskStatus(String taskId);

    /**
     * 获取导出文件路径
     *
     * @param taskId 任务ID
     * @return 文件路径
     */
    String getExportFilePath(String taskId);
}
