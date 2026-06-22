package com.hc.framework.rocketmq.core.transaction;

import com.hc.framework.rocketmq.core.BaseMqMessage;

/**
 * 事务消息日志存储接口。
 *
 * <p>用于在本地事务内持久化 {@link BaseMqMessage}，供事务回查时判断本地事务是否已提交。</p>
 *
 * <p><b>实现类必须保证 {@link #save(BaseMqMessage)} 操作与业务操作在<b>同一数据库事务</b>内执行</b>，
 * 这样才能利用数据库的原子性：事务未提交时回查查不到，事务已提交时回查能查到。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * rocketMqSender.sendTransaction("TOPIC", "TAG", data, ctx -> {
 *     transactionTemplate.executeWithoutResult(status -> {
 *         transactionLogStore.save(ctx.getMessage());  // 同一事务内持久化消息日志
 *         doLocalBusiness();                            // 同一事务内执行业务
 *     });
 * });
 * }</pre>
 *
 * @author hc-framework
 */
public interface TransactionLogStore {

    /**
     * 保存消息日志（应在本地事务内调用）。
     *
     * @param message 消息对象（含 msgId、topic、tag、data 等）
     */
    void save(BaseMqMessage message);

    /**
     * 根据 msgId 判断消息日志是否存在（用于事务回查）。
     *
     * @param msgId 消息 ID
     * @return true 表示消息日志存在（即本地事务已提交）
     */
    boolean existsByMsgId(String msgId);
}
