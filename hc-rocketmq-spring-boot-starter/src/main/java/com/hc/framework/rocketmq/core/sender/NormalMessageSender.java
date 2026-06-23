package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 普通消息发送器（同步 / 异步 / 单向）
 *
 * <p>使用示例：
 * <pre>{@code
 * // 同步发送
 * normalSender.send("OrderTopic", "created", orderDTO);
 *
 * // 异步发送（不等待结果）
 * normalSender.sendAsync("OrderTopic", "created", orderDTO);
 *
 * // 单向发送（不关心结果，适合日志等场景）
 * normalSender.sendOneway("OrderTopic", "log", logData);
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class NormalMessageSender extends AbstractMessageSender {

    private final RocketMQClientTemplate template;

    private final ExecutorService asyncExecutor;

    public NormalMessageSender(RocketMQClientTemplate template, boolean loggerEnabled) {
        super(loggerEnabled);
        this.template = template;
        this.asyncExecutor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                r -> {
                    Thread t = new Thread(r, "rocketmq-async");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // ====================== 同步发送 ======================

    /**
     * 同步发送普通消息。
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     * @return 发送回执
     */
    public SendReceipt send(String topic, String tag, Object data) {
        BaseMqMessage msg = buildMessage(data);
        String destination = buildDestination(topic, tag);
        try {
            if (loggerEnabled) {
                log.info("[RocketMQ] 发送普通消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());
            }
            SendReceipt receipt = template.syncSendNormalMessage(destination, msg);
            if (loggerEnabled) {
                log.info("[RocketMQ] 普通消息发送成功 msgId:{}", msg.getMsgId());
            }
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 普通消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 同步发送普通消息（无 tag）。
     */
    public SendReceipt send(String topic, Object data) {
        return send(topic, "*", data);
    }

    // ====================== 异步发送 ======================

    /**
     * 异步发送消息（不等待结果）。
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     */
    public void sendAsync(String topic, String tag, Object data) {
        BaseMqMessage msg = buildMessage(data);
        String destination = buildDestination(topic, tag);
        if (loggerEnabled) {
            log.info("[RocketMQ] 发送异步消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());
        }
        asyncExecutor.execute(() -> {
            try {
                template.syncSendNormalMessage(destination, msg);
                if (loggerEnabled) {
                    log.info("[RocketMQ] 异步消息发送成功 msgId:{}", msg.getMsgId());
                }
            } catch (Exception e) {
                log.error("[RocketMQ] 异步消息发送失败 msgId:{}", msg.getMsgId(), e);
            }
        });
    }

    /**
     * 异步发送消息（无 tag）。
     */
    public void sendAsync(String topic, Object data) {
        sendAsync(topic, "*", data);
    }

    // ====================== 单向发送 ======================

    /**
     * 单向发送（不等待响应，用于日志等低价值场景）。
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     */
    public void sendOneway(String topic, String tag, Object data) {
        BaseMqMessage msg = buildMessage(data);
        String destination = buildDestination(topic, tag);
        if (loggerEnabled) {
            log.info("[RocketMQ] 发送单向消息 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId());
        }
        asyncExecutor.execute(() -> {
            try {
                template.syncSendNormalMessage(destination, msg);
            } catch (Exception e) {
                log.warn("[RocketMQ] 单向消息发送失败（已忽略） msgId:{}", msg.getMsgId());
            }
        });
    }

    /**
     * 单向发送（无 tag）。
     */
    public void sendOneway(String topic, Object data) {
        sendOneway(topic, "*", data);
    }
}
