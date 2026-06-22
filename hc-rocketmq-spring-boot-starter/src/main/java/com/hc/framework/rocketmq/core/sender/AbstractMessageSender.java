package com.hc.framework.rocketmq.core.sender;

import com.hc.framework.rocketmq.core.BaseMqMessage;

import com.hc.framework.rocketmq.util.MdcUtils;

import java.util.UUID;

/**
 * 消息发送器抽象基类
 *
 * <p>封装所有类型 Sender 的公共逻辑：消息构建 / destination 构建 / 日志开关。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public abstract class AbstractMessageSender {

    /** 日志开关（由配置项 hc.rocketmq.producer-logger-enable 控制） */
    protected final boolean loggerEnabled;

    protected AbstractMessageSender(boolean loggerEnabled) {
        this.loggerEnabled = loggerEnabled;
    }

    /**
     * 构建消息信封（msgId + traceId + timestamp + data）。
     *
     * @param data 业务数据
     * @return 消息对象
     */
    protected BaseMqMessage buildMessage(Object data) {
        BaseMqMessage msg = new BaseMqMessage();
        msg.setMsgId(UUID.randomUUID().toString().replace("-", ""));
        String traceId = MdcUtils.getTraceId();
        if (traceId == null) {
            traceId = MdcUtils.generateTraceId();
        }
        msg.setTraceId(traceId);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setData(data);
        return msg;
    }

    /**
     * 构建 destination（topic:tag，tag 为 "*" 时仅返回 topic）。
     */
    protected String buildDestination(String topic, String tag) {
        if (tag == null || "*".equals(tag)) {
            return topic;
        }
        return topic + ":" + tag;
    }
}
