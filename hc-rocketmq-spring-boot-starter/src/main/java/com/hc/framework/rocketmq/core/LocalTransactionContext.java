package com.hc.framework.rocketmq.core;

import org.apache.rocketmq.client.apis.producer.Transaction;

/**
 * 事务消息本地事务上下文。
 *
 * <p>在 Lambda API 中传递给调用方，提供 {@link Transaction} 和 {@link BaseMqMessage}。</p>
 *
 * <p>调用方可通过 {@link #getMessage()} 获取消息对象（含 msgId、topic、tag），
 * 用于在同一事务内调用 {@link TransactionLogStore#save(BaseMqMessage)} 持久化消息日志。</p>
 *
 * @author hc-framework
 */
public class LocalTransactionContext {

    private final Transaction transaction;
    private final BaseMqMessage message;

    public LocalTransactionContext(Transaction transaction, BaseMqMessage message) {
        this.transaction = transaction;
        this.message = message;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public BaseMqMessage getMessage() {
        return message;
    }
}
