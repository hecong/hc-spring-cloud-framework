package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;
import com.hc.framework.rocketmq.core.transaction.TransactionLogStore;
import com.hc.framework.rocketmq.core.transaction.TransactionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.apache.rocketmq.client.common.Pair;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

/**
 * 事务消息发送器（框架自动管理 DB 事务边界 + MQ commit/rollback）
 *
 * <p>与 {@link RocketMqSender} 的区别：</p>
 * <ul>
 *     <li>RocketMqSender：原子 MQ 操作，业务方自己管理事务</li>
 *     <li>TransactionalMessageSender：编排 MQ 半消息 + DB 事务 + commit/rollback，<b>业务方只写业务逻辑</b></li>
 * </ul>
 *
 * <p><b>框架自动执行 5 步：</b></p>
 * <ol>
 *     <li>构建 {@link BaseMqMessage}（msgId/traceId/timestamp）</li>
 *     <li>发送半消息至 RocketMQ Broker</li>
 *     <li>通过 {@link TransactionTemplate} 开启本地 DB 事务</li>
 *     <li>在同一事务内：{@link TransactionLogStore#save(BaseMqMessage)} + 业务逻辑</li>
 *     <li>事务成功 → {@link Transaction#commit()}；事务失败 → {@link Transaction#rollback()}</li>
 * </ol>
 *
 * <p><b>使用示例（发送方和消费方共用同一个 DTO）：</b></p>
 * <pre>{@code
 * // === 定义消息 DTO（双方共用）===
 * public class OrderMessageDTO {
 *     private String orderNo;
 *     private BigDecimal amount;
 *     // getter/setter...
 * }
 *
 * // === 发送方 ===
 * @Service
 * public class OrderService {
 *     private final TransactionalMessageSender transactionalSender;
 *     private final OrderMapper orderMapper;
 *
 *     public void createOrder(OrderMessageDTO dto) {
 *         transactionalSender.sendInTransaction("OrderTopic", "created", dto, order -> {
 *             orderMapper.insert(order);  // 无强转，泛型已推导为 OrderMessageDTO
 *         });
 *     }
 * }
 *
 * // === 消费方 ===
 * @Component
 * @RocketMQMessageListener(topic = "OrderTopic", tag = "created")
 * public class OrderConsumer extends BaseMqConsumer<OrderMessageDTO> {
 *     @Override
 *     protected void doConsume(OrderMessageDTO order) {
 *         // 直接拿到 OrderMessageDTO，无需手动反序列化
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class TransactionalMessageSender extends AbstractMessageSender {

    private final RocketMQClientTemplate rocketMQClientTemplate;
    private final TransactionLogStore transactionLogStore;
    private final TransactionTemplate transactionTemplate;

    public TransactionalMessageSender(RocketMQClientTemplate rocketMQClientTemplate,
                                      TransactionLogStore transactionLogStore,
                                      TransactionTemplate transactionTemplate,
                                      boolean loggerEnabled) {
        super(loggerEnabled);
        this.rocketMQClientTemplate = rocketMQClientTemplate;
        this.transactionLogStore = transactionLogStore;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 发送事务消息（框架自动管理 DB 事务 + MQ commit/rollback）。
     *
     * <p>执行流程：</p>
     * <pre>
     * 发半消息 → Broker 持有半消息
     *   → 开 DB 事务 → saveLog + businessLogic
     *     → 成功 → commit 半消息（消息对消费者可见）
     *     → 失败 → rollback 半消息（消息被 Broker 丢弃）
     *
     * 异常路径：app 在事务提交和 commit 之间崩溃
     *   → Broker 超时回查 UniversalTransactionChecker
     *   → existsByMsgId(msgId) → false → ROLLBACK ✓
     * </pre>
     *
     * @param topic          消息主题
     * @param tag            消息标签
     * @param data           业务数据
     * @param businessLogic  业务逻辑（在事务内执行，无需手动管理事务或保存日志）
     * @param <T>            业务数据类型
     * @return TransactionResult（含 SendReceipt 和 Transaction）
     */
    public <T> TransactionResult sendInTransaction(String topic, String tag, T data,
                                                    Consumer<T> businessLogic) {
        // 1. 构建消息信封
        BaseMqMessage msg = buildMessage(data);
        msg.setTopic(topic);
        msg.setTag(tag);
        String destination = buildDestination(topic, tag);

        // 2. 发送半消息至 Broker
        if (loggerEnabled) {
            log.info("[RocketMQ] 发送事务半消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());
        }
        Pair<SendReceipt, Transaction> pair;
        try {
            pair = rocketMQClientTemplate.sendTransactionMessage(
                    destination, MessageBuilder.withPayload(msg).build());
        } catch (Exception e) {
            log.error("[RocketMQ] 事务半消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw new RuntimeException("事务消息发送失败", e);
        }
        if (loggerEnabled) {
            log.info("[RocketMQ] 事务半消息发送成功 msgId:{}", msg.getMsgId());
        }

        // 3 + 4. 本地事务：日志 + 业务，原子执行
        try {
            transactionTemplate.executeWithoutResult(status -> {
                transactionLogStore.save(msg);       // 框架自动存日志
                businessLogic.accept(data);          // 业务方只写这个
            });

            // 5a. DB 事务成功 → MQ commit（消息对消费者可见）
            pair.getTransaction().commit();
            if (loggerEnabled) {
                log.info("[RocketMQ] 事务消息提交成功 msgId:{}", msg.getMsgId());
            }
        } catch (Exception e) {
            // 5b. DB 事务失败 → MQ rollback（消息被 Broker 丢弃）
            log.error("[RocketMQ] 本地事务失败，回滚事务消息 msgId:{}", msg.getMsgId(), e);
            rollbackSafely(pair.getTransaction());
            throw e instanceof RuntimeException ? (RuntimeException) e
                    : new RuntimeException("事务消息发送失败", e);
        }

        return new TransactionResult(pair.getSendReceipt(), pair.getTransaction());
    }

    /**
     * 发送事务消息（无 tag）。
     */
    public <T> TransactionResult sendInTransaction(String topic, T data,
                                                    Consumer<T> businessLogic) {
        return sendInTransaction(topic, "*", data, businessLogic);
    }

    // ====================== 私有方法 ======================

    /**
     * 安全 rollback（吞掉 rollback 异常，不覆盖原始业务异常）。
     */
    private void rollbackSafely(Transaction transaction) {
        try {
            transaction.rollback();
        } catch (Exception e) {
            log.error("[RocketMQ] 事务消息 rollback 失败", e);
        }
    }
}
