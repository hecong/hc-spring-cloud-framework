package com.hc.framework.redis.util;

import lombok.RequiredArgsConstructor;
import org.dromara.hutool.core.collection.CollUtil;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 缓存工具类（适配自定义 JSON 序列化）
 *
 * @author hecong
 */
@RequiredArgsConstructor
public class RedisCacheUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 通用缓存操作 ====================

    /**
     * 放入缓存
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 放入缓存并设置过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除
     */
    public Long delete(List<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 根据前缀批量删除（SCAN 游标 + 分批 DELETE，避免 KEYS 阻塞 Redis 单线程）
     *
     * <p>单次扫描数量有界（{@link #SCAN_COUNT_LIMIT}），单批删除数量有界（{@link #DELETE_BATCH_SIZE}）。
     * 语义为尽力而为的最终一致：遍历期间新增/删除的 key 不做强一致承诺，删除幂等可重入。
     * 前缀匹配量极大时耗时随 key 数量线性增长（换取非阻塞收益），请谨慎评估调用频率。</p>
     */
    public Long deleteByPrefix(String prefix) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .match(prefix + "*")
            .count(SCAN_COUNT_LIMIT)
            .build();
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            long deleted = 0L;
            List<byte[]> batch = new ArrayList<>(DELETE_BATCH_SIZE);
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(scanOptions)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= DELETE_BATCH_SIZE) {
                        deleted += deleteBatch(connection, batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                deleted += deleteBatch(connection, batch);
            }
            return deleted;
        });
    }

    /**
     * 单次 DELETE 的批量上限
     */
    private static final int DELETE_BATCH_SIZE = 500;

    /**
     * 单次 SCAN 游标数量上限
     */
    private static final int SCAN_COUNT_LIMIT = 1000;

    private long deleteBatch(RedisConnection connection, List<byte[]> keys) {
        Long deleted = connection.keyCommands().del(keys.toArray(new byte[0][]));
        return deleted == null ? 0L : deleted;
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    // ==================== 原子操作 ====================

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== Hash 结构 ====================

    public void hPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void hPutAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }

    public Map<String, Object> hEntries(String key) {
        return redisTemplate.opsForHash().entries(key).entrySet().stream()
            .collect(Collectors.toMap(
                e -> String.valueOf(e.getKey()),
                Map.Entry::getValue
            ));
    }

    public Long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    // ==================== List 结构 ====================

    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T lPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }

    public void rPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    // ==================== Set 结构 ====================

    public void sAdd(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

}