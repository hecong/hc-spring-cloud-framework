package com.hc.framework.rocketmq.core;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * MQ 消息基础模型
 *
 * <p>所有 RocketMQ 消息的基类，包含通用字段：</p>
 * <ul>
 *     <li>msgId: 消息唯一标识（业务生成）</li>
 *     <li>traceId: 链路追踪 ID（用于日志追踪）</li>
 *     <li>timestamp: 消息发送时间戳</li>
 *     <li>data: 业务数据</li>
 * </ul>
 *
 * @author hc-framework
 */
@Data
public class BaseMqMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一标识（业务生成，用于幂等判断）
     */
    private String msgId;

    /**
     * 链路追踪 ID（用于日志追踪）
     */
    private String traceId;

    /**
     * 租户ID（为了后续扩展 租户功能，暂未使用）
     */
    private String tenantId;

    /**
     * 消息发送时间戳
     */
    private Long timestamp;

    /**
     * 业务数据
     */
    private Object data;

}
