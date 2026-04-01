package com.hnhegui.framework.excel.service;

import com.hnhegui.framework.excel.model.ExcelImportRequest;
import com.hnhegui.framework.excel.model.ExcelImportResult;
import com.hnhegui.framework.excel.model.ExcelTaskStatus;
import com.hnhegui.framework.excel.model.multisheet.MultiSheetImportBuilder;
import com.hnhegui.framework.excel.model.multisheet.MultiSheetImportResult;
import com.alibaba.excel.read.listener.ReadListener;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel导入服务接口
 */
public interface ExcelImportService {

    /**
     * 同步导入（小文件）
     *
     * <p>返回结果中包含所有成功导入的数据列表</p>
     *
     * @param inputStream 文件输入流
     * @param clazz        数据模型类
     * @param <T>          数据类型
     * @return 导入结果（包含 dataList）
     */
    <T> ExcelImportResult<T> importData(InputStream inputStream, Class<T> clazz);

    /**
     * 同步导入（小文件）
     *
     * @param request 导入请求
     * @param clazz  数据模型类
     * @param <T>    数据类型
     * @return 导入结果（包含 dataList）
     */
    <T> ExcelImportResult<T> importData(ExcelImportRequest request, Class<T> clazz);

    /**
     * 同步导入-自定义处理（小文件）
     *
     * <p>通过自定义 ReadListener 实现每读一行就处理的逻辑</p>
     *
     * @param inputStream  文件输入流
     * @param clazz         数据模型类
     * @param readListener  自定义读取监听器
     * @param <T>           数据类型
     */
    <T> void importData(InputStream inputStream, Class<T> clazz, ReadListener<T> readListener);

    /**
     * 同步导入-带批次处理（小文件）
     *
     * <p>每读取一批数据就执行 batchHandler 回调</p>
     *
     * @param inputStream  文件输入流
     * @param clazz         数据模型类
     * @param batchSize     批次大小
     * @param batchHandler  批次处理回调
     * @param <T>           数据类型
     */
    <T> void importData(InputStream inputStream, Class<T> clazz, int batchSize, Consumer<List<T>> batchHandler);

    /**
     * 异步导入（大文件）
     *
     * @param request            导入请求
     * @param clazz              数据模型类
     * @param batchHandler       批处理回调（每批数据处理）
     * @param progressCallback   进度回调
     * @param <T>                数据类型
     * @return 任务ID
     */
    <T> String importDataAsync(ExcelImportRequest request, Class<T> clazz,
                                Consumer<List<T>> batchHandler,
                                Consumer<Integer> progressCallback);

    /**
     * 异步导入-完整配置（大文件）
     *
     * @param request            导入请求
     * @param clazz              数据模型类
     * @param batchHandler       批处理回调（每批数据处理）
     * @param errorHandler       错误处理回调
     * @param progressCallback   进度回调
     * @param <T>                数据类型
     * @return 任务ID
     */
    <T> String importDataAsync(ExcelImportRequest request, Class<T> clazz,
                                Consumer<List<T>> batchHandler,
                                Consumer<ExcelImportResult.ErrorRow<T>> errorHandler,
                                Consumer<Integer> progressCallback);

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    ExcelTaskStatus getTaskStatus(String taskId);

    /**
     * 创建多Sheet导入构建器
     *
     * <p>支持链式配置多个Sheet的导入参数</p>
     *
     * @param inputStream 文件输入流
     * @return 多Sheet导入构建器
     */
    MultiSheetImportBuilder importMultiSheet(InputStream inputStream);

    /**
     * 执行多Sheet导入
     *
     * @param inputStream 文件输入流
     * @param builder     多Sheet导入构建器
     * @return 多Sheet导入结果
     */
    MultiSheetImportResult importMultiSheet(InputStream inputStream, MultiSheetImportBuilder builder);
}
