package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.transaction.TransactionResult;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * RocketMQ 消息发送门面（兼容旧 API，内部委托给独立 Sender）
 *
 * <p><b>设计意图：</b>本类仅作向后兼容门面。新代码推荐直接注入独立 Sender，
 * 语义更清晰，IDE 自动补全也更精确。</p>
 *
 * <p><b>迁移指南：</b></p>
 * <table>
 *   <tr><th>旧 API（门面）</th><th>新 API（独立 Sender）</th></tr>
 *   <tr><td>{@code rocketMqSender.send(topic, tag, data)}</td><td>{@code normalSender.send(topic, tag, data)}</td></tr>
 *   <tr><td>{@code rocketMqSender.sendDelay(topic, tag, data, 5, SECONDS)}</td><td>{@code delaySender.send(topic, tag, data, 5, SECONDS)}</td></tr>
 *   <tr><td>{@code rocketMqSender.sendOrderly(topic, tag, data, group)}</td><td>{@code fifoSender.send(topic, tag, data, group)}</td></tr>
 *   <tr><td>{@code rocketMqSender.sendBatch(topic, tag, list)}</td><td>{@code batchSender.send(topic, tag, list)}</td></tr>
 *   <tr><td>{@code rocketMqSender.sendTransaction(topic, tag, data, ctx -> {...})}</td><td>{@code transactionalSender.sendInTransaction(topic, tag, data, dto -> {...})}</td></tr>
 * </table>
 *
 * @author hc-framework
 */
public class RocketMqSender {

    private final NormalMessageSender normalSender;
    private final DelayMessageSender delaySender;
    private final FifoMessageSender fifoSender;
    private final BatchMessageSender batchSender;
    private final TransactionalMessageSender transactionalSender;

    /**
     * 所有 RocketMQClientTemplate Bean（用于多 Checker 事务场景）
     */
    private final Map<String, RocketMQClientTemplate> templateMap;

    public RocketMqSender(NormalMessageSender normalSender,
                          DelayMessageSender delaySender,
                          FifoMessageSender fifoSender,
                          BatchMessageSender batchSender,
                          TransactionalMessageSender transactionalSender,
                          Map<String, RocketMQClientTemplate> templateMap) {
        this.normalSender = normalSender;
        this.delaySender = delaySender;
        this.fifoSender = fifoSender;
        this.batchSender = batchSender;
        this.transactionalSender = transactionalSender;
        this.templateMap = templateMap;
    }

    // ====================== 普通消息 → NormalMessageSender ======================

    /** @see NormalMessageSender#send(String, String, Object) */
    public SendReceipt send(String topic, String tag, Object data) {
        return normalSender.send(topic, tag, data);
    }

    /** @see NormalMessageSender#send(String, Object) */
    public SendReceipt send(String topic, Object data) {
        return normalSender.send(topic, data);
    }

    // ====================== 异步消息 → NormalMessageSender ======================

    /** @see NormalMessageSender#sendAsync(String, String, Object) */
    public void sendAsync(String topic, String tag, Object data) {
        normalSender.sendAsync(topic, tag, data);
    }

    /** @see NormalMessageSender#sendAsync(String, Object) */
    public void sendAsync(String topic, Object data) {
        normalSender.sendAsync(topic, data);
    }

    // ====================== 单向消息 → NormalMessageSender ======================

    /** @see NormalMessageSender#sendOneway(String, String, Object) */
    public void sendOneway(String topic, String tag, Object data) {
        normalSender.sendOneway(topic, tag, data);
    }

    /** @see NormalMessageSender#sendOneway(String, Object) */
    public void sendOneway(String topic, Object data) {
        normalSender.sendOneway(topic, data);
    }

    // ====================== 延迟消息 → DelayMessageSender ======================

    /** @see DelayMessageSender#send(String, String, Object, long, TimeUnit) */
    public SendReceipt sendDelay(String topic, String tag, Object data, long delay, TimeUnit unit) {
        return delaySender.send(topic, tag, data, delay, unit);
    }

    /** @see DelayMessageSender#send(String, Object, long, TimeUnit) */
    public SendReceipt sendDelay(String topic, Object data, long delay, TimeUnit unit) {
        return delaySender.send(topic, data, delay, unit);
    }

    // ====================== 顺序消息 → FifoMessageSender ======================

    /** @see FifoMessageSender#send(String, String, Object, String) */
    public SendReceipt sendOrderly(String topic, String tag, Object data, String messageGroup) {
        return fifoSender.send(topic, tag, data, messageGroup);
    }

    /** @see FifoMessageSender#send(String, Object, String) */
    public SendReceipt sendOrderly(String topic, Object data, String messageGroup) {
        return fifoSender.send(topic, data, messageGroup);
    }

    // ====================== 批量消息 → BatchMessageSender ======================

    /** @see BatchMessageSender#send(String, String, Collection) */
    public int sendBatch(String topic, String tag, Collection<?> dataList) {
        return batchSender.send(topic, tag, dataList);
    }

    /** @see BatchMessageSender#send(String, Collection) */
    public int sendBatch(String topic, Collection<?> dataList) {
        return batchSender.send(topic, dataList);
    }

    // ====================== 事务消息 ======================

    /**
     * 发送事务消息（推荐）— 框架自动管理 DB 事务 + MQ commit/rollback。
     *
     * <p>委托给 {@link TransactionalMessageSender#sendInTransaction(String, String, Object, Consumer)}。</p>
     *
     * @param topic         消息主题
     * @param tag           消息标签
     * @param data          业务数据
     * @param businessLogic 业务逻辑（框架自动管理事务和日志）
     * @param <T>           业务数据类型
     * @return TransactionResult
     */
    public <T> TransactionResult sendTransaction(String topic, String tag, T data,
                                                  Consumer<T> businessLogic) {
        return transactionalSender.sendInTransaction(topic, tag, data, businessLogic);
    }

    /**
     * 发送事务消息（无 tag，推荐）。
     */
    public <T> TransactionResult sendTransaction(String topic, T data,
                                                  Consumer<T> businessLogic) {
        return transactionalSender.sendInTransaction(topic, data, businessLogic);
    }

    /**
     * 发送事务消息（仅发送半消息，业务方自行管理事务和 commit/rollback）。
     *
     * @deprecated 请使用 {@link #sendTransaction(String, String, Object, Consumer)}，
     *             由框架自动管理事务。仅在不使用 Spring TransactionTemplate 的遗留场景保留。
     */
    @Deprecated
    public TransactionResult sendTransaction(String topic, String tag, Object data) {
        return transactionalSender.sendInTransaction(topic, tag, data, d -> {});
    }

    /**
     * @deprecated 请使用 {@link #sendTransaction(String, Object, Consumer)}。
     */
    @Deprecated
    public TransactionResult sendTransaction(String topic, Object data) {
        return sendTransaction(topic, "*", data);
    }

    // ====================== 多 Template 事务（保留给多 Checker 场景） ======================

    /**
     * 使用指定 Template 发送事务消息（多 Checker 场景）。
     *
     * <p>templateBeanName 需与 Checker 上
     * {@code @RocketMQTransactionListener(rocketMQTemplateBeanName = "xxx")} 的值一致。</p>
     */
    public <T> TransactionResult sendTransaction(String templateBeanName, String topic, String tag,
                                                  T data, Consumer<T> businessLogic) {
        if (!templateMap.containsKey(templateBeanName)) {
            throw new IllegalArgumentException(
                    "未找到 RocketMQ Template Bean: " + templateBeanName
                    + "。可用 Template: " + templateMap.keySet());
        }
        return transactionalSender.sendInTransaction(topic, tag, data, businessLogic);
    }
}
