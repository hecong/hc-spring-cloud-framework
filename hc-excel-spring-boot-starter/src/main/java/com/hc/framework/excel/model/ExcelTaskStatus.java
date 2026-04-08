package com.hc.framework.excel.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

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
    private AtomicInteger processed;

    /**
     * 成功数量
     */
    private AtomicInteger successCount;

    /**
     * 失败数量
     */
    private AtomicInteger failCount;

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
        this.processed = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.failCount = new AtomicInteger(0);
        this.progress = 0;
        this.startTime = System.currentTimeMillis();
    }

    public void updateProgress(int total) {
        this.total = total;
        this.state = TaskState.RUNNING;
        if (total > 0) {
            this.progress = (int) ((processed.get() * 100.0) / total);
        }
    }

    public void incrementSuccess() {
        this.successCount.incrementAndGet();
        this.processed.incrementAndGet();
    }

    public void incrementFail() {
        this.failCount.incrementAndGet();
        this.processed.incrementAndGet();
    }

    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.progress = 100;
        this.state = this.failCount.get() > 0 && this.successCount.get() == 0 ? TaskState.FAIL : TaskState.SUCCESS;
    }

    public void fail(String errorMsg) {
        this.endTime = System.currentTimeMillis();
        this.state = TaskState.FAIL;
        this.errorMsg = errorMsg;
    }
}
