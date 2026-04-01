package com.hc.framework.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author hecong
 * @since 2026/4/1 15:53
 */
@Slf4j
public class LockTemplate {

    /**
     * 默认等待时间（秒），-1表示无限等待
     */
    private static final int DEFAULT_WAIT_TIME = -1;
    
    /**
     * 默认租约时间（秒），-1表示自动续期
     */
    private static final int DEFAULT_LEASE_TIME = -1;
    
    /**
     * 默认错误提示
     */
    private static final String DEFAULT_ERROR_MSG = "获取锁失败，请稍后再试。";

    private final RedissonClient redissonClient;

    public LockTemplate(RedissonClient redissonClient){
        this.redissonClient = redissonClient;
    }

    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param action 业务方法接口实现
     */
    @Nullable
    public <T> T execute(String lockKey, Callable<T> action) {
        return this.execute(lockKey, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param action 业务方法接口实现
     */
    @Nullable
    public void execute(String lockKey, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param action 业务方法接口实现
     */
    @Nullable
    public <T> T execute(String lockKey, String errorMsg, Callable<T> action) {
        return this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param action 业务方法接口实现
     */
    @Nullable
    public void execute(String lockKey, String errorMsg, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    @Nullable
    public void execute(String lockKey, String errorMsg, LockType lockType, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    @Nullable
    public <T> T execute(String lockKey, String errorMsg, LockType lockType, Callable<T> action) {
        return this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param action 业务方法接口实现
     */
    @Nullable
    @Deprecated
    public <T> T execute(String lockKey, int waitTime, Callable<T> action) {
        return this.execute(lockKey, waitTime, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param action 业务方法接口实现
     */
    @Nullable
    @Deprecated
    public <T> T execute(String lockKey, int waitTime, String errorMsg, Callable<T> action) {
        return this.execute(lockKey, waitTime, errorMsg, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param leaseTime 锁释放时间
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    @Nullable
    private <T> T execute(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Callable<T> action) {
        return this.handleLock(lockKey, waitTime, errorMsg, leaseTime, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param leaseTime 锁释放时间
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    @Nullable
    private void execute(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Runnable action) {
        this.handleLock(lockKey, waitTime, errorMsg, leaseTime, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param errorMsg 获取锁超时，抛出ServiceException异常信息
     * @param leaseTime 锁释放时间
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    @Nullable
    private <T> T handleLock(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Object action) {
        // 加锁
        RLock lock = this.getLock(lockKey, lockType);
        boolean isLock = false;
        try {
            isLock = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                if (action instanceof Callable<?> callable) {
                    @SuppressWarnings("unchecked")
                    T result = (T) callable.call();
                    return result;
                } else if (action instanceof Runnable) {
                    ((Runnable) action).run();
                    return null;
                } else {
                    throw new IllegalArgumentException("参数错误，只能是 Runnable or Callable");
                }
            }
            // 加锁失败，执行抛异常
            throw new LockException(LockException.LOCK_BUSY, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } catch (InterruptedException e) {
            log.error("获取锁被中断 lockKey={}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new LockException(LockException.LOCK_ACQUIRE_FAILED, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } catch (LockException e) {
            log.warn("获取锁超时或失败，@Lock位置：{}，lockKey={}，waitTime={}",
                action.getClass().getSimpleName(), lockKey, waitTime);
            throw e;
        } catch (Exception e) {
            log.error("执行业务逻辑异常 lockKey={}", lockKey, e);
            throw new RuntimeException(e);
        } finally {
            // 解锁
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKeys 锁的key值 数组
     * @param action 业务方法接口实现
     */
    @Nullable
    public void execute(Collection<String> lockKeys, Runnable action) {
        this.handleLock(lockKeys, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    @Nullable
    public <T> T execute(Collection<String> lockKeys, Callable<T> action) {
        return this.handleLock(lockKeys, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    @Nullable
    private <T> T handleLock(Collection<String> lockKeys, int waitTime, String errorMsg, int leaseTime, LockType lockType, Object action) {
        // 加锁
        List<RLock> lockList = lockKeys.stream().map(key -> this.getLock(key, lockType)).toList();
        RLock multiLock = redissonClient.getMultiLock(lockList.toArray(new RLock[0]));
        boolean isLock = false;
        try {
            isLock = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (isLock) {
                if (action instanceof Callable<?> callable) {
                    @SuppressWarnings("unchecked")
                    T result = (T) callable.call();
                    return result;
                } else if (action instanceof Runnable) {
                    ((Runnable) action).run();
                    return null;
                } else {
                    throw new IllegalArgumentException("参数错误，只能是 Runnable or Callable");
                }
            }
            // 加锁失败，执行抛异常
            throw new LockException(LockException.LOCK_BUSY, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } catch (InterruptedException e) {
            log.error("获取锁被中断 lockKeys={}", lockKeys, e);
            Thread.currentThread().interrupt();
            throw new LockException(LockException.LOCK_ACQUIRE_FAILED, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } catch (LockException e) {
            log.warn("获取锁超时或失败，@Lock位置：{}，lockKeys={}，waitTime={}",
                action.getClass().getSimpleName(), lockKeys, waitTime);
            throw e;
        } catch (Exception e) {
            log.error("执行业务逻辑异常 lockKeys={}", lockKeys, e);
            throw new RuntimeException(e);
        } finally {
            // 解锁
            if (isLock && multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    public boolean isLocked(String lockKey){
        RLock lock = this.getLock(lockKey, LockType.REDIS_REENTRANT_LOCK);
        return lock.isLocked();
    }

    public boolean isLocked(String lockKey, LockType lockType){
        RLock lock = this.getLock(lockKey, lockType);
        return lock.isLocked();
    }


    private RLock getLock(String lockKey, LockType type) {
        return switch (type) {
            case REDIS_FAIR_LOCK -> redissonClient.getFairLock(lockKey);
            case REDIS_SPIN_LOCK -> redissonClient.getSpinLock(lockKey);
            case REDIS_REENTRANT_LOCK -> redissonClient.getLock(lockKey);
            case REDIS_READ_LOCK-> redissonClient.getReadWriteLock(lockKey).readLock();
            case REDIS_WRITE_LOCK -> redissonClient.getReadWriteLock(lockKey).writeLock();
        };
    }
}