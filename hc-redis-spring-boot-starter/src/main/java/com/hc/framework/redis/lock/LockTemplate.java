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
 * 基于 Redisson 的分布式锁模板。
 *
 * <p><b>异常语义（持锁回调）：</b></p>
 * <ul>
 *   <li>{@link RuntimeException}（含业务异常、参数校验异常、{@link LockException}）——原样向外抛出，不做包装；</li>
 *   <li>{@link InterruptedException}——先恢复当前线程中断标志，再抛 {@code LockException(LOCK_INTERRUPTED)}；</li>
 *   <li>其他受检异常/未知异常——包装为 {@code LockException(LOCK_EXECUTION_FAILED)}，原始异常作为 cause 保留。</li>
 * </ul>
 * 获取锁等待期间被中断：恢复中断标志并抛 {@code LockException(LOCK_ACQUIRE_FAILED)}。
 * 无论成功、异常还是中断，锁都会在退出前可靠释放。
 *
 * @author hecong
 * @since 2026/4/1 15:53
 */
@Slf4j
public class LockTemplate {

    /**
     * 默认等待时间（秒），30秒超时避免线程无限阻塞
     */
    private static final int DEFAULT_WAIT_TIME = 30;
    
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
    public <T> T execute(String lockKey, Callable<T> action) {
        return this.execute(lockKey, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param action 业务方法接口实现
     */
    public void execute(String lockKey, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param action 业务方法接口实现
     */
    public <T> T execute(String lockKey, String errorMsg, Callable<T> action) {
        return this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param action 业务方法接口实现
     */
    public void execute(String lockKey, String errorMsg, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    public void execute(String lockKey, String errorMsg, LockType lockType, Runnable action) {
        this.execute(lockKey, DEFAULT_WAIT_TIME, errorMsg, DEFAULT_LEASE_TIME, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁。自动续期、可重入锁
     * @param lockKey 锁的key值
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
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
     * @param errorMsg 获取锁超时，抛出LockException异常信息
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
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param leaseTime 锁释放时间
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    private <T> T execute(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Callable<T> action) {
        return this.handleLock(lockKey, waitTime, errorMsg, leaseTime, lockType, action);
    }

    /**
     * 基于RedissonClient的分布式锁
     * @param lockKey 锁的key值
     * @param waitTime 获取锁等待时间
     * @param errorMsg 获取锁超时，抛出LockException异常信息
     * @param leaseTime 锁释放时间
     * @param lockType 锁类型 {@link LockType}
     * @param action 业务方法接口实现
     */
    private void execute(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Runnable action) {
        this.handleLock(lockKey, waitTime, errorMsg, leaseTime, lockType, action);
    }

    /**
     * 单把锁：获取锁失败抛 {@code LOCK_BUSY}/等待中断抛 {@code LOCK_ACQUIRE_FAILED}，
     * 持锁回调异常按 {@link LockTemplate} 类注释中的异常语义处理，锁在 finally 中释放。
     */
    private <T> T handleLock(String lockKey, int waitTime, String errorMsg, int leaseTime, LockType lockType, Object action) {
        RLock lock = this.getLock(lockKey, lockType);
        boolean isLock = false;
        try {
            try {
                isLock = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取锁被中断 lockKey={}", lockKey, e);
                throw new LockException(LockException.LOCK_ACQUIRE_FAILED,
                    errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG, e);
            }
            if (isLock) {
                return runLockedAction(lockKey, action);
            }
            // 加锁失败（等待超时/锁被占用），执行抛异常
            log.warn("获取锁超时或失败 lockKey={}, waitTime={}", lockKey, waitTime);
            throw new LockException(LockException.LOCK_BUSY, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } finally {
            // 解锁（线程中断场景同样可靠释放）
            if (isLock) {
                releaseIfHeldByCurrentThread(lock);
            }
        }
    }


    /**
     * 基于RedissonClient的分布式锁。默认的错误提示、自动续期、可重入锁
     * @param lockKeys 锁的key值 数组
     * @param action 业务方法接口实现
     */
    public void execute(Collection<String> lockKeys, Runnable action) {
        this.handleLock(lockKeys, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    public <T> T execute(Collection<String> lockKeys, Callable<T> action) {
        return this.handleLock(lockKeys, DEFAULT_WAIT_TIME, null, DEFAULT_LEASE_TIME, LockType.REDIS_REENTRANT_LOCK, action);
    }

    /**
     * 多把锁（MultiLock）：异常语义与单把锁一致，锁在 finally 中释放。
     */
    private <T> T handleLock(Collection<String> lockKeys, int waitTime, String errorMsg, int leaseTime, LockType lockType, Object action) {
        List<RLock> lockList = lockKeys.stream().map(key -> this.getLock(key, lockType)).toList();
        RLock multiLock = redissonClient.getMultiLock(lockList.toArray(new RLock[0]));
        boolean isLock = false;
        try {
            try {
                isLock = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("获取锁被中断 lockKeys={}", lockKeys, e);
                throw new LockException(LockException.LOCK_ACQUIRE_FAILED,
                    errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG, e);
            }
            if (isLock) {
                return runLockedAction(lockKeys, action);
            }
            // 加锁失败（等待超时/锁被占用），执行抛异常
            log.warn("获取锁超时或失败 lockKeys={}, waitTime={}", lockKeys, waitTime);
            throw new LockException(LockException.LOCK_BUSY, errorMsg != null ? errorMsg : DEFAULT_ERROR_MSG);
        } finally {
            // 解锁（线程中断场景同样可靠释放）
            if (isLock) {
                releaseIfHeldByCurrentThread(multiLock);
            }
        }
    }

    /**
     * 释放锁（若为当前线程持有）。
     *
     * <p>Redisson 的同步命令（如 isHeldByCurrentThread）基于 Future#get 阻塞等待，
     * 当前线程中断标志置位时会立即抛中断相关异常，从而掩盖真正要抛出的异常并导致锁泄漏。
     * 因此在执行释放前临时清除中断标志，释放完成后恢复中断位。</p>
     */
    private void releaseIfHeldByCurrentThread(RLock lock) {
        boolean wasInterrupted = Thread.currentThread().isInterrupted();
        try {
            if (wasInterrupted) {
                Thread.interrupted();
            }
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 持锁执行回调并统一处理异常语义：
     * RuntimeException 原样抛出；InterruptedException 恢复中断位后抛 LOCK_INTERRUPTED；
     * 其余异常包装为 LOCK_EXECUTION_FAILED。
     */
    @SuppressWarnings("unchecked")
    private <T> T runLockedAction(Object lockDescription, Object action) {
        try {
            if (action instanceof Callable<?> callable) {
                return (T) callable.call();
            } else if (action instanceof Runnable runnable) {
                runnable.run();
                return null;
            } else {
                throw new IllegalArgumentException("参数错误，只能是 Runnable or Callable");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("持锁执行业务逻辑被中断 lockKey={}", lockDescription, e);
            throw new LockException(LockException.LOCK_INTERRUPTED, "持锁执行业务逻辑被中断", e);
        } catch (RuntimeException e) {
            // 业务异常/参数异常等运行时异常原样透传，保证全局异常处理器可识别
            throw e;
        } catch (Exception e) {
            log.error("持锁执行业务逻辑异常 lockKey={}", lockDescription, e);
            throw new LockException(LockException.LOCK_EXECUTION_FAILED, "持锁执行业务逻辑失败", e);
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
