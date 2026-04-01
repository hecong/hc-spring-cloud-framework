package com.hc.framework.excel.service.impl;

import com.hc.framework.excel.service.ExcelOperationRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Excel操作记录器空实现
 *
 * <p>默认空实现，仅输出DEBUG日志。引用方可以实现 {@link ExcelOperationRecorder} 接口
 * 并注册为Spring Bean来覆盖此实现，实现自定义的记录逻辑（如保存到数据库、发送消息等）。</p>
 *
 * <p>此实现由 {@code ExcelAutoConfiguration} 通过 @Bean 显式注册，
 * 确保在引用项目不能扫描到框架包时仍能正常工作。</p>
 *
 * <h3>自定义实现示例（使用 ExcelOperatorResolver 获取操作人信息）：</h3>
 * <pre>{@code
 * @Component
 * public class DbExcelOperationRecorder implements ExcelOperationRecorder {
 *     @Autowired
 *     private ExcelOperationLogMapper logMapper;
 *     @Autowired
 *     private ExcelOperatorResolver operatorResolver;  // 注入操作人解析器
 *
 *     @Override
 *     public void recordExport(String taskId, ExcelExportRequest request, int dataCount) {
 *         ExcelOperationLog log = new ExcelOperationLog();
 *         log.setTaskId(taskId);
 *         log.setOperationType("EXPORT");
 *         log.setFileName(request.getFileName());
 *         log.setDataCount(dataCount);
 *         // 从解析器获取当前操作人
 *         log.setOperatorId(operatorResolver.getOperatorId());
 *         log.setOperatorName(operatorResolver.getOperatorName());
 *         log.setCreateTime(LocalDateTime.now());
 *         logMapper.insert(log);
 *     }
 *
 *     @Override
 *     public void recordExportComplete(String taskId, boolean success, String fileUrl, String errorMsg) {
 *         // 更新任务状态和文件URL
 *         ExcelOperationLog log = logMapper.selectByTaskId(taskId);
 *         if (log != null) {
 *             log.setSuccess(success);
 *             log.setFileUrl(fileUrl);
 *             log.setErrorMsg(errorMsg);
 *             log.setCompleteTime(LocalDateTime.now());
 *             logMapper.updateById(log);
 *         }
 *     }
 *     // ... 其他方法实现
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class NoOpExcelOperationRecorder implements ExcelOperationRecorder {

    private static final Logger log = LoggerFactory.getLogger(NoOpExcelOperationRecorder.class);


    @Override
    public void recordExportComplete(String taskId, String operatorId, String operatorName, boolean success, String fileUrl, String errorMsg) {
        if (log.isDebugEnabled()) {
            log.debug("[Excel导出]完成 - taskId: {}, operatorId: {}, operatorName: {}, success: {}, fileUrl: {}, error: {}",
                    taskId, operatorId, operatorName, success, fileUrl, errorMsg);
        }
    }


    @Override
    public void recordImportComplete(String taskId, String operatorId, String operatorName,  boolean success, int successCount,
                                     int failCount, String errorMsg) {
        if (log.isDebugEnabled()) {
            log.debug("[Excel导入]完成 - taskId: {}, operatorId: {}, operatorName: {}, success: {}, successCount: {}, failCount: {}, error: {}",
                    taskId, operatorId, operatorName, success, successCount, failCount, errorMsg);
        }
    }

}
