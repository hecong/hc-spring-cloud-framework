package com.hc.framework.rocketmq.core.transaction;

import com.hc.framework.rocketmq.core.BaseMqMessage;
import com.hc.framework.rocketmq.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;
import org.springframework.core.ResolvableType;

/**
 * 事务消息回查基类（泛型版）
 *
 * <p>
 * 操作业务 DTO，无需手动调用 {@code OrderMessageDTO.from(msg)}。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Component
 * @RocketMQTransactionListener(rocketMQTemplateBeanName = "orderPayTransTemplate")
 * public class OrderPayChecker extends BaseTransactionChecker<OrderMessageDTO> {
 *     @Override
 *     protected boolean doCheckTransaction(OrderMessageDTO dto) {
 *         // 直接使用 dto，无需手动反序列化
 *         return orderPayMapper.selectByOrderNo(dto.getOrderNo()) != null;
 *     }
 * }
 * }</pre>
 *
 * @param <T> 业务数据类型
 * @author hc-framework
 */
@Slf4j
public abstract class BaseTransactionChecker<T> implements TransactionChecker {

    /**
     * 自动解析的泛型类型（构造时一次性解析缓存）。
     */
    @SuppressWarnings("unchecked")
    private final Class<T> dataType = (Class<T>) ResolvableType.forClass(getClass())
        .as(BaseTransactionChecker.class)
        .getGeneric(0)
        .resolve();

    public BaseTransactionChecker() {
        if (dataType == null) {
            throw new IllegalStateException(
                "无法解析 " + getClass().getName() + " 的泛型参数。"
                    + "请确保子类明确指定了 BaseTransactionChecker<T> 的泛型类型。");
        }
    }

    @Override
    public TransactionResolution check(MessageView messageView) {
        BaseMqMessage msg = MessageUtils.parseMessage(messageView);
        if (msg == null) {
            log.error("[RocketMQ] 事务消息解析失败");
            return TransactionResolution.ROLLBACK;
        }

        try {
            log.info("[RocketMQ] 事务回查 msgId:{}", msg.getMsgId());
            T data = msg.getDataAs(dataType);
            boolean committed = doCheckTransaction(data);
            log.info("[RocketMQ] 事务回查结果 msgId:{} committed:{}", msg.getMsgId(), committed);
            return committed ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;
        } catch (Exception e) {
            log.error("[RocketMQ] 事务回查异常 msgId:{}", msg.getMsgId(), e);
            return TransactionResolution.UNKNOWN;
        }
    }

    /**
     * 回查本地事务状态（由 RocketMQ 服务端回调）。
     *
     * @param data 已反序列化的业务数据
     * @return true 表示已提交，false 表示已回滚
     */
    protected abstract boolean doCheckTransaction(T data);
}
