package com.hc.framework.rocketmq.core;

import com.hc.framework.common.util.JsonUtils;
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
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 业务数据
     */
    private Object data;

    /**
     * 将 data 字段转换为指定类型（Consumer 和 Checker 共用）。
     *
     * <p>支持三种输入形式：</p>
     * <ul>
     *     <li>目标类型的实例 → 直接转型</li>
     *     <li>Map 对象 → 通过 {@link JsonUtils#fromMap} 转换</li>
     *     <li>其他对象 → JSON 序列化后反序列化为目标类型</li>
     * </ul>
     *
     * @param <T>  目标类型
     * @param type 目标类型的 Class 对象
     * @return 转换后的业务数据，data 为 null 时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> type) {
        if (data == null) {
            return null;
        }
        if (type.isInstance(data)) {
            return (T) data;
        }
        if (data instanceof java.util.Map) {
            return JsonUtils.fromMap((java.util.Map<?, ?>) data, type);
        }
        return JsonUtils.fromJson(JsonUtils.toJson(data), type);
    }

}
