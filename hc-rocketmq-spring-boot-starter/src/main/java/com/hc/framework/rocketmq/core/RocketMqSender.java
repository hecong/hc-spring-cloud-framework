package com.hc.framework.rocketmq.core;

import com.hc.framework.rocketmq.util.MdcUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.apache.rocketmq.client.common.Pair;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ 消息发送器
 *
 * <p>封装 RocketMQ 5.x gRPC 版本的消息发送，支持：</p>
 * <ul>
 *     <li>普通消息（同步）</li>
 *     <li>延迟消息</li>
 *     <li>顺序消息</li>
 *     <li>批量消息</li>
 *     <li>异步消息</li>
 *     <li>单向消息</li>
 *     <li>事务消息</li>
 * </ul>
 *
 * @author hc-framework
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqSender {

    private final RocketMQClientTemplate rocketMQClientTemplate;

    /**
     * 所有 RocketMQClientTemplate Bean（Spring 自动按 beanName 收集）
     *
     * <p>用于事务消息指定不同 Checker 的场景：key=beanName，value=Template 实例。</p>
     */
    private final Map<String, RocketMQClientTemplate> templateMap;

    /**
     * 异步发送线程池（有界队列 + CallerRunsPolicy 防止 OOM）
     */
    private final ExecutorService asyncExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            r -> {
                Thread t = new Thread(r, "rocketmq-async-sender");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 构建消息对象
     *
     * @param data 业务数据
     * @return BaseMqMessage
     */
    private BaseMqMessage build(Object data) {
        BaseMqMessage msg = new BaseMqMessage();
        msg.setMsgId(UUID.randomUUID().toString().replace("-", ""));
        // 优先使用当前线程的 traceId，如果不存在则生成新的
        String traceId = MdcUtils.getTraceId();
        if (traceId == null) {
            traceId = MdcUtils.generateTraceId();
        }
        msg.setTraceId(traceId);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setData(data);
        return msg;
    }

    // ====================== 1. 普通消息 ======================

    /**
     * 发送普通消息（同步）
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     * @return SendReceipt 发送回执
     */
    public SendReceipt send(String topic, String tag, Object data) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        try {
            log.info("[RocketMQ] 发送普通消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());
            SendReceipt receipt = rocketMQClientTemplate.syncSendNormalMessage(destination, msg);
            log.info("[RocketMQ] 普通消息发送成功 msgId:{}", msg.getMsgId());
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 普通消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 发送普通消息（同步，无 tag）
     *
     * @param topic 主题
     * @param data  业务数据
     * @return SendReceipt 发送回执
     */
    public SendReceipt send(String topic, Object data) {
        return send(topic, "*", data);
    }

    // ====================== 2. 延迟消息 ======================

    /**
     * 发送延迟消息
     *
     * @param topic  主题
     * @param tag    标签
     * @param data   业务数据
     * @param delay  延迟时间
     * @param unit   时间单位
     * @return SendReceipt 发送回执
     */
    public SendReceipt sendDelay(String topic, String tag, Object data, long delay, java.util.concurrent.TimeUnit unit) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        try {
            Duration duration = Duration.ofMillis(unit.toMillis(delay));
            log.info("[RocketMQ] 发送延迟消息 topic:{} tag:{} delay:{}ms msgId:{}",
                    topic, tag, unit.toMillis(delay), msg.getMsgId());
            SendReceipt receipt = rocketMQClientTemplate.syncSendDelayMessage(destination, msg, duration);
            log.info("[RocketMQ] 延迟消息发送成功 msgId:{}", msg.getMsgId());
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 延迟消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 发送延迟消息（无 tag）
     *
     * @param topic  主题
     * @param data   业务数据
     * @param delay  延迟时间
     * @param unit   时间单位
     * @return SendReceipt 发送回执
     */
    public SendReceipt sendDelay(String topic, Object data, long delay, java.util.concurrent.TimeUnit unit) {
        return sendDelay(topic, "*", data, delay, unit);
    }

    // ====================== 3. 顺序消息 ======================

    /**
     * 发送顺序消息
     *
     * <p>相同 messageGroup 的消息会进入同一个队列，保证先入先出</p>
     * <p>典型场景：同一订单的所有操作（创建、支付、完成）需要顺序执行</p>
     *
     * @param topic        主题
     * @param tag          标签
     * @param data         业务数据
     * @param messageGroup 消息组标识（相同组的消息顺序消费，如：订单ID、用户ID）
     * @return SendReceipt 发送回执
     */
    public SendReceipt sendOrderly(String topic, String tag, Object data, String messageGroup) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        try {
            log.info("[RocketMQ] 发送顺序消息 topic:{} tag:{} messageGroup:{} msgId:{}",
                topic, tag, messageGroup, msg.getMsgId());
            // 使用官方 Starter 的顺序消息 API
            SendReceipt receipt = rocketMQClientTemplate.syncSendFifoMessage(destination, msg, messageGroup);
            log.info("[RocketMQ] 顺序消息发送成功 msgId:{}", msg.getMsgId());
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 顺序消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 发送顺序消息（无 tag）
     *
     * @param topic   主题
     * @param data    业务数据
     * @param messageGroup 消息组标识（相同组的消息顺序消费，如：订单ID、用户ID）
     * @return SendReceipt 发送回执
     */
    public SendReceipt sendOrderly(String topic, Object data, String messageGroup) {
        return sendOrderly(topic, "*", data, messageGroup);
    }

    // ====================== 4. 批量消息 ======================

    /**
     * 发送批量消息（同步）
     *
     * <p>注意：批量消息会逐个发送，RocketMQ 5.x gRPC 版本暂不支持原生批量发送</p>
     *
     * @param topic    主题
     * @param tag      标签
     * @param dataList 业务数据列表
     */
    public int sendBatch(String topic, String tag, Collection<?> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        log.info("[RocketMQ] 发送批量消息 topic:{} tag:{} size:{}", topic, tag, dataList.size());
        int successCount = 0;
        for (Object data : dataList) {
            try {
                send(topic, tag, data);
                successCount++;
            } catch (Exception e) {
                log.error("[RocketMQ] 批量消息中某条发送失败", e);
            }
        }
        log.info("[RocketMQ] 批量消息发送完成，成功 {}/{}", successCount, dataList.size());
        return successCount;
    }

    /**
     * 发送批量消息（无 tag）
     *
     * @param topic    主题
     * @param dataList 业务数据列表
     * @return 成功发送的消息数
     */
    public int sendBatch(String topic, Collection<?> dataList) {
        return sendBatch(topic, "*", dataList);
    }

    // ====================== 5. 异步消息 ======================

    /**
     * 发送异步消息
     *
     * <p>注意：RocketMQ 5.x gRPC 版本使用同步发送模拟异步</p>
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     */
    public void sendAsync(String topic, String tag, Object data) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        log.info("[RocketMQ] 发送异步消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());

        // 使用线程池执行发送
        asyncExecutor.execute(() -> {
            try {
                SendReceipt receipt = rocketMQClientTemplate.syncSendNormalMessage(destination, msg);
                log.info("[RocketMQ] 异步消息发送成功 msgId:{}", msg.getMsgId());
            } catch (Exception e) {
                log.error("[RocketMQ] 异步消息发送失败 msgId:{}", msg.getMsgId(), e);
            }
        });
    }

    /**
     * 发送异步消息（无 tag）
     *
     * @param topic 主题
     * @param data  业务数据
     */
    public void sendAsync(String topic, Object data) {
        sendAsync(topic, "*", data);
    }

    // ====================== 6. 单向消息 ======================

    /**
     * 发送单向消息（不等待服务器响应，用于日志收集等场景）
     *
     * <p>注意：单向消息使用异步发送且不等待结果</p>
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     */
    public void sendOneway(String topic, String tag, Object data) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        log.info("[RocketMQ] 发送单向消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());

        // 使用线程池执行发送，不等待结果
        asyncExecutor.execute(() -> {
            try {
                rocketMQClientTemplate.syncSendNormalMessage(destination, msg);
            } catch (Exception e) {
                log.warn("[RocketMQ] 单向消息发送失败（已忽略） msgId:{}", msg.getMsgId());
            }
        });
    }

    /**
     * 发送单向消息（无 tag）
     *
     * @param topic 主题
     * @param data  业务数据
     */
    public void sendOneway(String topic, Object data) {
        sendOneway(topic, "*", data);
    }

    // ====================== 7. 事务消息 ======================

    /**
     * 发送事务消息
     *
     * <p>事务消息发送后，需要业务层根据本地事务执行结果调用 {@link Transaction#commit()} 或 {@link Transaction#rollback()}</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * TransactionResult result = rocketMqSender.sendTransaction("TOPIC", "TAG", data);
     * try {
     *     // 执行本地事务（如数据库操作）
     *     doLocalTransaction();
     *     result.getTransaction().commit();
     * } catch (Exception e) {
     *     result.getTransaction().rollback();
     * }
     * }</pre>
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     * @return TransactionResult 包含 SendReceipt 和 Transaction
     */
    /**
     * 发送事务消息（使用默认 Template，适用于单 Checker 场景）
     */
    public TransactionResult sendTransaction(String topic, String tag, Object data) {
        return sendTransaction(rocketMQClientTemplate, topic, tag, data);
    }

    /**
     * 发送事务消息（指定 Template Bean 名称，适用于多 Checker 场景）
     *
     * <p>templateBeanName 需与 Checker 上
     * {@code @RocketMQTransactionListener(rocketMQTemplateBeanName = "xxx")} 的值一致。</p>
     *
     * <pre>{@code
     * // 发送时指定模板名
     * rocketMqSender.sendTransaction("orderPayRocketMQClientTemplate", "OrderTopic", "paid", order);
     *
     * // Checker 上写对应的模板名
     * @RocketMQTransactionListener(rocketMQTemplateBeanName = "orderPayRocketMQClientTemplate")
     * public class OrderPayChecker extends BaseTransactionChecker { ... }
     * }</pre>
     *
     * @param templateBeanName RocketMQClientTemplate 的 Bean 名称
     * @param topic            主题
     * @param tag              标签
     * @param data             业务数据
     * @return TransactionResult 包含 SendReceipt 和 Transaction
     * @throws IllegalArgumentException 如果指定名称的 Template 不存在
     */
    public TransactionResult sendTransaction(String templateBeanName,
            String topic, String tag, Object data) {
        RocketMQClientTemplate template = templateMap.get(templateBeanName);
        if (template == null) {
            throw new IllegalArgumentException(
                    "未找到 RocketMQ Template Bean: " + templateBeanName
                    + "。请检查 @RocketMQTransactionListener 的 rocketMQTemplateBeanName 是否与此处一致。"
                    + " 可用 Template: " + templateMap.keySet());
        }
        return sendTransaction(template, topic, tag, data);
    }

    private TransactionResult sendTransaction(RocketMQClientTemplate template,
            String topic, String tag, Object data) {
        BaseMqMessage msg = build(data);
        String destination = buildDestination(topic, tag);
        try {
            log.info("[RocketMQ] 发送事务消息 topic:{} tag:{} msgId:{}",
                    topic, tag, msg.getMsgId());
            Pair<SendReceipt, Transaction> pair = template.sendTransactionMessage(
                    destination,
                    MessageBuilder.withPayload(msg).build()
            );
            log.info("[RocketMQ] 事务消息发送成功 msgId:{}", msg.getMsgId());
            return new TransactionResult(pair.getSendReceipt(), pair.getTransaction());
        } catch (Exception e) {
            log.error("[RocketMQ] 事务消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw new RuntimeException("事务消息发送失败", e);
        }
    }

    /**
     * 发送事务消息（无 tag）
     *
     * @param topic 主题
     * @param data  业务数据
     * @return TransactionResult 包含 SendReceipt 和 Transaction
     */
    public TransactionResult sendTransaction(String topic, Object data) {
        return sendTransaction(topic, "*", data);
    }

    // ====================== 私有方法 ======================

    /**
     * 构建目标地址（topic:tag）
     *
     * @param topic 主题
     * @param tag   标签
     * @return topic:tag 格式字符串
     */
    private String buildDestination(String topic, String tag) {
        if (tag == null || "*".equals(tag)) {
            return topic;
        }
        return topic + ":" + tag;
    }

    /**
     * 销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
                log.warn("[RocketMQ] 异步发送线程池强制关闭");
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
