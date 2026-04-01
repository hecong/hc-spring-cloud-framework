package com.hnhegui.framework.excel.service;

import com.hnhegui.framework.excel.model.ExcelExportRequest;
import com.hnhegui.framework.excel.model.ExcelImportRequest;

import java.util.List;
import java.util.Map;

/**
 * Excel操作记录器接口
 *
 * <p>用于记录Excel导入导出操作日志，支持自定义实现。</p>
 * <p>引用方可以通过实现此接口并注册为Spring Bean来覆盖默认的空实现。</p>
 * <p>操作人信息通过 {@link ExcelOperatorResolver} 获取，实现类可以自行注入使用。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface ExcelOperationRecorder {


    /**
     * 记录导出完成
     *
     * @param taskId     任务ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param success    是否成功
     * @param fileUrl    文件URL（云端地址）
     * @param errorMsg   错误信息（失败时）
     */
    void recordExportComplete(String taskId, String operatorId, String operatorName, boolean success, String fileUrl, String errorMsg);


    /**
     * 记录导入完成
     *
     * @param taskId       任务ID
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param success      是否成功
     * @param successCount 成功条数
     * @param failCount    失败条数
     * @param errorMsg     错误信息（失败时）
     */
    void recordImportComplete(String taskId, String operatorId, String operatorName, boolean success, int successCount, int failCount,
                              String errorMsg);
}
