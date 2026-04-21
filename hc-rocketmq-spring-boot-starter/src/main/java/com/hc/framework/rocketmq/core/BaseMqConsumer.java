package com.hc.framework.rocketmq.core;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.util.IdempotentUtils;
import com.hc.framework.rocketmq.util.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 通用 MQ 消费者基类（适配 RocketMQ 5.x 官方 Starter）
 *
 * <p>实现 RocketMQListener 接口，子类只需实现 doConsume 方法</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Component
 * @RocketMQMessageListener(
 *     topic = "OrderTopic",
 *     tag = "create",
 *     consumerGroup = "order-group"
 * )
 * public class OrderConsumer extends BaseMqConsumer<OrderDTO> {
 *     @Override
 *     protected Class<OrderDTO> getDataType() {
 *         return OrderDTO.class;
 *     }
 *
 *     @Override
 *     protected void doConsume(OrderDTO order) {
 *         // 业务处理
 *     }
 * }
 * }</pre>
 *
 * @param <T> 业务数据类型
 * @author hc-framework
 */
@Slf4j
public abstract class BaseMqConsumer<T> implements RocketMQListener {

    @Autowired(required = false)
    private IdempotentUtils idempotentUtils;

    /**
     * 消费消息（实现 RocketMQListener 接口）
     */
    @Override
    public ConsumeResult consume(MessageView messageView) {
        String body = parseMessageBody(messageView.getBody());
        BaseMqMessage baseMsg;

        try {
            // 1. 解析消息
            baseMsg = JsonUtils.fromJson(body, BaseMqMessage.class);
            if (baseMsg == null) {
                log.error("[RocketMQ] 消息解析失败，body: {}", body);
                return ConsumeResult.FAILURE;
            }

            // 2. 设置链路追踪 ID
            String traceId = baseMsg.getTraceId();
            if (traceId != null) {
                MdcUtils.setTraceId(traceId);
            }

            String msgId = baseMsg.getMsgId();
            String topic = messageView.getTopic();
            String tag = messageView.getTag().orElse(null);

            log.info("[RocketMQ] 开始消费消息 topic:{} tag:{} msgId:{}", topic, tag, msgId);

            // 3. 幂等检查（如果已消费过，直接返回成功）
            if (idempotentUtils != null && idempotentUtils.isConsumed(msgId)) {
                log.warn("[RocketMQ] 消息重复消费，跳过处理 msgId:{}", msgId);
                return ConsumeResult.SUCCESS;
            }

            // 4. 转换业务数据并执行消费
            T businessData = convertData(baseMsg.getData());
            doConsume(businessData);

            // 5. 记录消费成功（幂等）
            if (idempotentUtils != null) {
                idempotentUtils.markConsumed(msgId);
            }

            log.info("[RocketMQ] 消息消费成功 topic:{} tag:{} msgId:{}", topic, tag, msgId);
            return ConsumeResult.SUCCESS;

        } catch (Exception e) {
            log.error("[RocketMQ] 消息消费异常", e);
            // 返回 FAILURE，框架会自动重试
            return ConsumeResult.FAILURE;
        } finally {
            MdcUtils.remove();
        }
    }

    /**
     * 转换业务数据
     *
     * @param data 原始数据
     * @return 转换后的业务数据
     */
    @SuppressWarnings("unchecked")
    protected T convertData(Object data) {
        // 默认实现：假设 data 是 Map 类型，转换为泛型 T
        if (data instanceof java.util.Map) {
            return JsonUtils.fromMap((java.util.Map<?, ?>) data, getDataType());
        }
        // 如果 data 已经是目标类型，直接返回
        if (getDataType().isInstance(data)) {
            return (T) data;
        }
        // 否则尝试 JSON 转换
        return JsonUtils.fromJson(JsonUtils.toJson(data), getDataType());
    }

    /**
     * 获取业务数据类型（用于自动转换）
     *
     * @return 业务数据类型
     */
    protected abstract Class<T> getDataType();

    /**
     * 子类实现具体的业务消费逻辑
     *
     * @param data 业务数据
     */
    protected abstract void doConsume(T data);

    /**
     * 安全地解析消息体 ByteBuffer 为字符串
     */
    private String parseMessageBody(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return new String(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), StandardCharsets.UTF_8);
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}