package com.hc.framework.excel.service.impl;

import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.service.ExcelTaskStore;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis任务存储实现（分布式模式）
 *
 * <p>使用Redis存储任务状态，支持多实例部署时共享任务数据。</p>
 * <p>自动设置TTL，默认1小时过期。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class RedisExcelTaskStore implements ExcelTaskStore {

    private static final String TASK_KEY_PREFIX = "excel:task:";
    private static final String EXPORT_KEY_PREFIX = "excel:export:";

    /** 默认TTL：1小时 */
    private static final long DEFAULT_TTL_HOURS = 1;

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisExcelTaskStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveTask(String taskId, ExcelTaskStatus status) {
        redisTemplate.opsForValue().set(taskKey(taskId), status, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public ExcelTaskStatus getTask(String taskId) {
        Object obj = redisTemplate.opsForValue().get(taskKey(taskId));
        return obj instanceof ExcelTaskStatus ? (ExcelTaskStatus) obj : null;
    }

    @Override
    public void removeTask(String taskId) {
        redisTemplate.delete(taskKey(taskId));
    }

    @Override
    public void saveExportFileUrl(String taskId, String fileUrl) {
        redisTemplate.opsForValue().set(exportKey(taskId), fileUrl, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public String getExportFileUrl(String taskId) {
        Object obj = redisTemplate.opsForValue().get(exportKey(taskId));
        return obj != null ? obj.toString() : null;
    }

    @Override
    public void removeExportFileUrl(String taskId) {
        redisTemplate.delete(exportKey(taskId));
    }

    private String taskKey(String taskId) {
        return TASK_KEY_PREFIX + taskId;
    }

    private String exportKey(String taskId) {
        return EXPORT_KEY_PREFIX + taskId;
    }
}
