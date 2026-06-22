package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 延迟消息发送器
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class DelayMessageSender extends AbstractMessageSender {

    private final RocketMQClientTemplate template;

    public DelayMessageSender(RocketMQClientTemplate template, boolean loggerEnabled) {
        super(loggerEnabled);
        this.template = template;
    }

    /**
     * 发送延迟消息。
     *
     * @param topic 主题
     * @param tag   标签
     * @param data  业务数据
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return 发送回执
     */
    public SendReceipt send(String topic, String tag, Object data, long delay, TimeUnit unit) {
        BaseMqMessage msg = buildMessage(data);
        String destination = buildDestination(topic, tag);
        Duration duration = Duration.ofMillis(unit.toMillis(delay));
        try {
            if (loggerEnabled) {
                log.info("[RocketMQ] 发送延迟消息 topic:{} tag:{} delay:{}ms msgId:{}",
                        topic, tag, unit.toMillis(delay), msg.getMsgId());
            }
            SendReceipt receipt = template.syncSendDelayMessage(destination, msg, duration);
            if (loggerEnabled) {
                log.info("[RocketMQ] 延迟消息发送成功 msgId:{}", msg.getMsgId());
            }
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 延迟消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 发送延迟消息（无 tag）。
     */
    public SendReceipt send(String topic, Object data, long delay, TimeUnit unit) {
        return send(topic, "*", data, delay, unit);
    }
}
