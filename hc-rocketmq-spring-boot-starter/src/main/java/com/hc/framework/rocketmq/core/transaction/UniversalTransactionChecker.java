package com.hc.framework.rocketmq.core.transaction;

import com.hc.framework.rocketmq.core.BaseMqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 通用事务消息回查 Checker。
 *
 * <p>基于 {@link TransactionLogStore} 判断本地事务是否已提交：</p>
 * <ul>
 *     <li>查到消息日志 → 事务已提交 → {@code return true}（commit）</li>
 *     <li>查不到 → 事务未提交或已回滚 → {@code return false}（rollback）</li>
 * </ul>
 *
 * <p>使用此 Checker 后，不再需要为每个事务场景单独编写 Checker。
 * 所有使用默认 Template 的事务消息共用此通用回查逻辑。</p>
 *
 * <p><b>注册方式</b>：由 {@code RocketMQAutoConfiguration} 在存在
 * {@link TransactionLogStore} Bean 时自动注册，绑定到默认 Template
 * {@code rocketMQClientTemplate}。用户不需要也不应该手动创建 Bean。</p>
 *
 * @author hc-framework
 */
@Slf4j
public class UniversalTransactionChecker extends BaseTransactionChecker<BaseMqMessage> {

    private final TransactionLogStore transactionLogStore;

    public UniversalTransactionChecker(
            @Autowired(required = false) TransactionLogStore transactionLogStore) {
        this.transactionLogStore = transactionLogStore;
    }

    @Override
    protected boolean doCheckTransaction(BaseMqMessage data) {
        if (transactionLogStore == null) {
            log.warn("[RocketMQ] 未找到 TransactionLogStore Bean，通用 Checker 降级为 rollback");
            return false;
        }
        boolean exists = transactionLogStore.existsByMsgId(data.getMsgId());
        log.info("[RocketMQ] 通用回查 msgId:{} → {}", data.getMsgId(), exists ? "commit" : "rollback");
        return exists;
    }
}
