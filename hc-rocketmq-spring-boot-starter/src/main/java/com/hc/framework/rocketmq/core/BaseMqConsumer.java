package com.hc.framework.rocketmq.core;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.util.IdempotentUtils;
import com.hc.framework.rocketmq.util.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 通用 MQ 消费者基类（适配 RocketMQ 5.x 官方 Starter）
 *
 * <p>实现 RocketMQListener 接口，子类只需实现 doConsume 方法。泛型参数 T 会被自动解析，
 * 无需重写 {@link #getDataType()}。</p>
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
     * 自动解析的泛型类型（构造时一次性解析缓存）。
     *
     * <p>支持多级继承，例如 {@code class SubConsumer extends OrderConsumer extends BaseMqConsumer<X>}，
     * 会沿着继承链找到 {@link BaseMqConsumer} 的第一个泛型参数。</p>
     */
    private final Class<T> resolvedDataType = resolveDataType();

    @SuppressWarnings("unchecked")
    private Class<T> resolveDataType() {
        Class<?> resolved = ResolvableType.forClass(getClass())
                .as(BaseMqConsumer.class)
                .getGeneric(0)
                .resolve();
        if (resolved == null) {
            throw new IllegalStateException(
                    "无法解析 " + getClass().getName() + " 的泛型参数。"
                    + "请确保子类明确指定了 BaseMqConsumer<T> 的泛型类型，"
                    + "或重写 getDataType() 方法手动指定。");
        }
        return (Class<T>) resolved;
    }

    /**
     * 消费消息（实现 RocketMQListener 接口）
     */
    @Override
    public ConsumeResult consume(MessageView messageView) {
        String body = parseMessageBody(messageView.getBody());
        BaseMqMessage baseMsg = null;

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

            // 3. 幂等检查（原子标记，首次消费返回 true，重复返回 false）
            if (idempotentUtils != null && !idempotentUtils.tryMarkConsumed(msgId)) {
                log.warn("[RocketMQ] 消息重复消费，跳过处理 msgId:{}", msgId);
                return ConsumeResult.SUCCESS;
            }

            // 4. 转换业务数据并执行消费
            T businessData = convertData(baseMsg.getData());
            doConsume(businessData);

            log.info("[RocketMQ] 消息消费成功 topic:{} tag:{} msgId:{}", topic, tag, msgId);
            return ConsumeResult.SUCCESS;

        } catch (Exception e) {
            log.error("[RocketMQ] 消息消费异常", e);
            // 失败时清除幂等标记，允许重试
            if (idempotentUtils != null && baseMsg != null) {
                idempotentUtils.remove(baseMsg.getMsgId());
            }
            // 返回 FAILURE，框架会自动重试
            return ConsumeResult.FAILURE;
        } finally {
            MdcUtils.remove();
        }
    }

    /**
     * 转换业务数据（委托到 {@link BaseMqMessage#getDataAs(Class)}）。
     *
     * @param data 原始数据
     * @return 转换后的业务数据
     */
    protected T convertData(Object data) {
        BaseMqMessage tmp = new BaseMqMessage();
        tmp.setData(data);
        return tmp.getDataAs(getDataType());
    }

    /**
     * 获取业务数据类型（用于自动转换）。
     *
     * <p>默认返回子类继承 {@code BaseMqConsumer<T>} 时声明的泛型类型（通过反射自动解析）。
     * 子类无需重写此方法；仅当泛型类型无法被反射解析（如使用原始类型继承）时才需要手动重写。</p>
     *
     * @return 业务数据类型
     * @deprecated 框架已自动解析泛型类型，子类无需重写。保留方法签名仅为向后兼容。
     */
    @Deprecated
    protected Class<T> getDataType() {
        return resolvedDataType;
    }

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