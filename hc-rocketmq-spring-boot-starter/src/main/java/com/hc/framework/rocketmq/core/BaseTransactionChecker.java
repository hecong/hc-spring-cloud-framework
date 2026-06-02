package com.hc.framework.rocketmq.core;

import com.hc.framework.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 事务消息监听器基类（适配官方 Starter）
 *
 * <p>子类需要添加 @RocketMQTransactionListener 注解：</p>
 * <p>注意：事务监听器需要配合自定义的 RocketMQClientTemplate 使用: 例如
 *  <li>订单创建事务监听器需要配合订单的 @RocketMQTransactionListener(rocketMQTemplateBeanName = "orderCreateRocketMQClientTemplate")</li>
 *  <li>订单支付事务监听器需要配合订单的 @RocketMQTransactionListener(rocketMQTemplateBeanName = "orderPayRocketMQClientTemplate")</li>
 * <pre>{@code
 * @Component
 * @RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQClientTemplate")
 * public class MyTransactionChecker extends BaseTransactionChecker {
 *     // 实现抽象方法
 * }
 * }</pre>
 */
@Slf4j
public abstract class BaseTransactionChecker implements TransactionChecker {

    @Override
    public TransactionResolution check(MessageView messageView) {
        BaseMqMessage msg = parseMessage(messageView);
        if (msg == null) {
            log.error("[RocketMQ] 事务消息解析失败");
            return TransactionResolution.ROLLBACK;
        }

        try {
            log.info("[RocketMQ] 事务回查 msgId:{}", msg.getMsgId());
            boolean committed = doCheckTransaction(msg);
            log.info("[RocketMQ] 事务回查结果 msgId:{} committed:{}", msg.getMsgId(), committed);
            return committed ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;
        } catch (Exception e) {
            log.error("[RocketMQ] 事务回查异常 msgId:{}", msg.getMsgId(), e);
            return TransactionResolution.UNKNOWN;
        }
    }

    /**
     * 安全解析消息体
     */
    private BaseMqMessage parseMessage(MessageView messageView) {
        try {
            ByteBuffer buffer = messageView.getBody();
            byte[] bytes;
            if (buffer.hasArray()) {
                // 安全截取有效字节（避免包含后备数组中的垃圾数据）
                bytes = new byte[buffer.remaining()];
                System.arraycopy(buffer.array(),
                        buffer.arrayOffset() + buffer.position(),
                        bytes, 0, buffer.remaining());
            } else {
                bytes = new byte[buffer.remaining()];
                buffer.duplicate().get(bytes); // 使用副本，避免修改原始 buffer 位置
            }
            String body = new String(bytes, StandardCharsets.UTF_8);
            return JsonUtils.fromJson(body, BaseMqMessage.class);
        } catch (Exception e) {
            log.error("[RocketMQ] 消息解析失败", e);
            return null;
        }
    }

    /**
     * 回查本地事务状态（由 RocketMQ 服务端回调）
     *
     * @param msg 消息对象
     * @return true 表示已提交，false 表示已回滚
     */
    protected abstract boolean doCheckTransaction(BaseMqMessage msg);
}