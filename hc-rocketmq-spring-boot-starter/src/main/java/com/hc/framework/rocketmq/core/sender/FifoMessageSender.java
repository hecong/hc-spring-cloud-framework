package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;

/**
 * 顺序消息发送器
 *
 * <p>相同 messageGroup 的消息会进入同一队列，保证先入先出。
 * 典型场景：同一订单的创建→支付→完成。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class FifoMessageSender extends AbstractMessageSender {

    private final RocketMQClientTemplate template;

    public FifoMessageSender(RocketMQClientTemplate template, boolean loggerEnabled) {
        super(loggerEnabled);
        this.template = template;
    }

    /**
     * 发送顺序消息。
     *
     * @param topic        主题
     * @param tag          标签
     * @param data         业务数据
     * @param messageGroup 消息分组（相同分组顺序消费）
     * @return 发送回执
     */
    public SendReceipt send(String topic, String tag, Object data, String messageGroup) {
        BaseMqMessage msg = buildMessage(data);
        String destination = buildDestination(topic, tag);
        try {
            if (loggerEnabled) {
                log.info("[RocketMQ] 发送顺序消息 topic:{} tag:{} messageGroup:{} msgId:{}",
                        topic, tag, messageGroup, msg.getMsgId());
            }
            SendReceipt receipt = template.syncSendFifoMessage(destination, msg, messageGroup);
            if (loggerEnabled) {
                log.info("[RocketMQ] 顺序消息发送成功 msgId:{}", msg.getMsgId());
            }
            return receipt;
        } catch (Exception e) {
            log.error("[RocketMQ] 顺序消息发送失败 topic:{} tag:{} msgId:{}", topic, tag, msg.getMsgId(), e);
            throw e;
        }
    }

    /**
     * 发送顺序消息（无 tag）。
     */
    public SendReceipt send(String topic, Object data, String messageGroup) {
        return send(topic, "*", data, messageGroup);
    }
}
