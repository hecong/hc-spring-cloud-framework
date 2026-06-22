package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * 批量消息发送器
 *
 * <p>内部复用 {@link NormalMessageSender} 逐条发送。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class BatchMessageSender extends AbstractMessageSender {

    private final NormalMessageSender normalSender;

    public BatchMessageSender(NormalMessageSender normalSender, boolean loggerEnabled) {
        super(loggerEnabled);
        this.normalSender = normalSender;
    }

    /**
     * 批量发送消息。
     *
     * @param topic    主题
     * @param tag      标签
     * @param dataList 业务数据列表
     * @return 发送成功的条数
     */
    public int send(String topic, String tag, Collection<?> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }
        if (loggerEnabled) {
            log.info("[RocketMQ] 发送批量消息 topic:{} tag:{} size:{}", topic, tag, dataList.size());
        }
        int successCount = 0;
        for (Object data : dataList) {
            try {
                normalSender.send(topic, tag, data);
                successCount++;
            } catch (Exception e) {
                log.error("[RocketMQ] 批量消息中某条发送失败", e);
            }
        }
        if (loggerEnabled) {
            log.info("[RocketMQ] 批量消息发送完成，成功 {}/{}", successCount, dataList.size());
        }
        return successCount;
    }

    /**
     * 批量发送消息（无 tag）。
     */
    public int send(String topic, Collection<?> dataList) {
        return send(topic, "*", dataList);
    }
}
