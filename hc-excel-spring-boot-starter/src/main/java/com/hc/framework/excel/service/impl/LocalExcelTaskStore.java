package com.hc.framework.excel.service.impl;

import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.service.ExcelTaskStore;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地任务存储实现（单机模式）
 *
 * <p>使用ConcurrentHashMap存储任务状态，适用于单实例部署。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class LocalExcelTaskStore implements ExcelTaskStore {

    private final ConcurrentHashMap<String, ExcelTaskStatus> taskStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> exportFileStore = new ConcurrentHashMap<>();

    @Override
    public void saveTask(String taskId, ExcelTaskStatus status) {
        taskStore.put(taskId, status);
    }

    @Override
    public ExcelTaskStatus getTask(String taskId) {
        return taskStore.get(taskId);
    }

    @Override
    public void removeTask(String taskId) {
        taskStore.remove(taskId);
    }

    @Override
    public void saveExportFileUrl(String taskId, String fileUrl) {
        exportFileStore.put(taskId, fileUrl);
    }

    @Override
    public String getExportFileUrl(String taskId) {
        return exportFileStore.get(taskId);
    }

    @Override
    public void removeExportFileUrl(String taskId) {
        exportFileStore.remove(taskId);
    }
}
