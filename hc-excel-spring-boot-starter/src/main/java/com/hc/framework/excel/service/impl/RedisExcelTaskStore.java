package com.hc.framework.excel.service.impl;

import com.hc.framework.excel.config.ExcelTaskStoreProperties;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.service.ExcelTaskStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis任务存储实现（分布式模式）
 *
 * <p>使用Redis存储任务状态，支持多实例部署时共享任务数据。</p>
 * <p>TTL 可通过 {@code hc.excel.task-store.ttl-hours} 配置，默认1小时。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class RedisExcelTaskStore implements ExcelTaskStore {

    private final String taskKeyPrefix;
    private final String exportKeyPrefix;
    private final long ttlHours;

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisExcelTaskStore(RedisTemplate<String, Object> redisTemplate, ExcelTaskStoreProperties props) {
        this.redisTemplate = redisTemplate;
        this.taskKeyPrefix = props.getTaskKeyPrefix();
        this.exportKeyPrefix = props.getExportKeyPrefix();
        this.ttlHours = props.getTtlHours();
    }

    @Override
    public void saveTask(String taskId, ExcelTaskStatus status) {
        redisTemplate.opsForValue().set(taskKey(taskId), status, ttlHours, TimeUnit.HOURS);
    }

    @Override
    public ExcelTaskStatus getTask(String taskId) {
        Object obj = redisTemplate.opsForValue().get(taskKey(taskId));
        if (obj instanceof ExcelTaskStatus status) {
            return status;
        }
        if (obj != null) {
            log.warn("Redis 中任务状态类型不匹配，期望 ExcelTaskStatus，实际: {}，taskId: {}",
                    obj.getClass().getName(), taskId);
        }
        return null;
    }

    @Override
    public void removeTask(String taskId) {
        redisTemplate.delete(taskKey(taskId));
    }

    @Override
    public void saveExportFileUrl(String taskId, String fileUrl) {
        redisTemplate.opsForValue().set(exportKey(taskId), fileUrl, ttlHours, TimeUnit.HOURS);
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
        return taskKeyPrefix + taskId;
    }

    private String exportKey(String taskId) {
        return exportKeyPrefix + taskId;
    }
}
