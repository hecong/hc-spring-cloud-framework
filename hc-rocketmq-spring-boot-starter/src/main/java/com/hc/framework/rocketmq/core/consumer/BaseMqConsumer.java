package com.hc.framework.rocketmq.core.consumer;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.core.BaseMqMessage;
import com.hc.framework.rocketmq.util.IdempotentUtils;
import com.hc.framework.rocketmq.util.MdcUtils;
import com.hc.framework.rocketmq.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;

/**
 * 通用 MQ 消费者基类（适配 RocketMQ 5.x 官方 Starter）
 *
 * <p>实现 RocketMQListener 接口，子类只需实现 doConsume 方法。
 * 泛型参数 T 通过反射自动解析，无需任何额外配置。</p>
 *
 * <p>使用方式（与发送方共用同一个 DTO）：</p>
 * <pre>{@code
 * @Component
 * @RocketMQMessageListener(topic = "OrderTopic", tag = "created")
 * public class OrderConsumer extends BaseMqConsumer<OrderMessageDTO> {
 *     @Override
 *     protected void doConsume(OrderMessageDTO order) {
 *         // 直接拿到 OrderMessageDTO，无需手动反序列化
 *         orderMapper.insert(order);
 *     }
 * }
 * }</pre>
 * <p>topic 和 consumerGroup 留空时可从 {@code spring.application.name} 自动推导，
 * endpoints 从 {@code rocketmq.producer.endpoints} 自动填充。</p>
 *
 * @param <T> 业务数据类型
 * @author hc-framework
 */
@Slf4j
public abstract class BaseMqConsumer<T> implements RocketMQListener {

    @Autowired(required = false)
    private IdempotentUtils idempotentUtils;

    /**
     * 消费者日志开关（由配置项 hc.rocketmq.consumer-logger-enable 控制，默认 true）。
     * 仅控制 info 级别日志，error/warn 始终输出。
     */
    @Value("${hc.rocketmq.consumer-logger-enable:true}")
    private boolean loggerEnabled;

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
                    + "请确保子类明确指定了 BaseMqConsumer<T> 的泛型类型。");
        }
        return (Class<T>) resolved;
    }

    /**
     * 消费消息（实现 RocketMQListener 接口）
     */
    @Override
    public ConsumeResult consume(MessageView messageView) {
        String body = MessageUtils.byteBufferToString(messageView.getBody());
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

            if (loggerEnabled) {
                log.info("[RocketMQ] 开始消费消息 topic:{} tag:{} msgId:{}", topic, tag, msgId);
            }

            // 3. 幂等检查（原子标记，首次消费返回 true，重复返回 false）
            if (idempotentUtils != null && !idempotentUtils.tryMarkConsumed(msgId)) {
                log.warn("[RocketMQ] 消息重复消费，跳过处理 msgId:{}", msgId);
                return ConsumeResult.SUCCESS;
            }

            // 4. 转换业务数据并执行消费
            T businessData = convertData(baseMsg.getData());
            doConsume(businessData);

            if (loggerEnabled) {
                log.info("[RocketMQ] 消息消费成功 topic:{} tag:{} msgId:{}", topic, tag, msgId);
            }
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
        return tmp.getDataAs(resolvedDataType);
    }

    /**
     * 子类实现具体的业务消费逻辑
     *
     * @param data 业务数据
     */
    protected abstract void doConsume(T data);

}