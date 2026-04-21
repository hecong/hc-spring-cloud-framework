package com.hc.framework.excel.service;

import com.hc.framework.excel.model.ExcelTaskStatus;

/**
 * Excel任务存储接口
 *
 * <p>抽象任务状态的存储方式，支持本地存储（单机）和Redis存储（分布式）。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface ExcelTaskStore {

    /**
     * 保存任务状态
     *
     * @param taskId 任务ID
     * @param status 任务状态
     */
    void saveTask(String taskId, ExcelTaskStatus status);

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态，不存在返回null
     */
    ExcelTaskStatus getTask(String taskId);

    /**
     * 移除任务状态
     *
     * @param taskId 任务ID
     */
    void removeTask(String taskId);

    /**
     * 保存导出文件URL
     *
     * @param taskId  任务ID
     * @param fileUrl 文件URL
     */
    void saveExportFileUrl(String taskId, String fileUrl);

    /**
     * 获取导出文件URL
     *
     * @param taskId 任务ID
     * @return 文件URL，不存在返回null
     */
    String getExportFileUrl(String taskId);

    /**
     * 移除导出文件URL
     *
     * @param taskId 任务ID
     */
    void removeExportFileUrl(String taskId);
}
