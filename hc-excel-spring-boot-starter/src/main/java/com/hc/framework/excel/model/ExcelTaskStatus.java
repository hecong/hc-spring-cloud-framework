package com.hc.framework.excel.model;

import lombok.Data;

/**
 * Excel异步任务状态
 */
@Data
public class ExcelTaskStatus {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务类型：IMPORT/EXPORT
     */
    private TaskType taskType;

    /**
     * 任务状态：PENDING/RUNNING/SUCCESS/FAIL
     */
    private TaskState state;

    /**
     * 总数量
     */
    private int total;

    /**
     * 已处理数量
     */
    private int processed;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 进度百分比
     */
    private int progress;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 文件路径（导出用）
     */
    private String filePath;

    /**
     * 任务开始时间
     */
    private long startTime;

    /**
     * 任务结束时间
     */
    private long endTime;

    public enum TaskType {
        IMPORT, EXPORT
    }

    public enum TaskState {
        PENDING, RUNNING, SUCCESS, FAIL
    }

    public ExcelTaskStatus(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.state = TaskState.PENDING;
        this.processed = 0;
        this.successCount = 0;
        this.failCount = 0;
        this.progress = 0;
        this.startTime = System.currentTimeMillis();
    }

    public void updateProgress(int total) {
        this.total = total;
        this.state = TaskState.RUNNING;
        if (total > 0) {
            this.progress = (int) ((processed * 100.0) / total);
        }
    }

    public synchronized void incrementSuccess() {
        this.successCount++;
        this.processed++;
    }

    public synchronized void incrementFail() {
        this.failCount++;
        this.processed++;
    }

    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.progress = 100;
        this.state = this.failCount > 0 && this.successCount == 0 ? TaskState.FAIL : TaskState.SUCCESS;
    }

    public void fail(String errorMsg) {
        this.endTime = System.currentTimeMillis();
        this.state = TaskState.FAIL;
        this.errorMsg = errorMsg;
    }
}
