package com.hc.framework.rocketmq.example;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.core.BaseMqMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 顺序消息消费者示例
 *
 * <p>注意：顺序消息需要设置 consumeThreadNumber = 1，确保单线程消费</p>
 * <p>注意：RocketMQ 5.x gRPC 版本的消费者注解路径不同，请使用官方配置方式</p>
 *
 * @author hc-framework
 */
@Slf4j
@Component
// RocketMQ 5.x gRPC 版本的消费者配置方式请参考官方文档
// @RocketMQMessageListener(
//         topic = "TEST_ORDER_TOPIC",
//         tag = "TEST_TAG",
//         consumerGroup = "GID_TEST_ORDER",
//         consumeThreadNumber = 1  // 顺序消息必须单线程消费
// )
public class OrderlyMessageConsumer extends BaseOrderlyMqConsumer {

    @Override
    protected void doConsume(BaseMqMessage message) {
        // 将 data 转换为具体类型
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) message.getData();
        TestMessageDTO dto = JsonUtils.fromMap(dataMap, TestMessageDTO.class);

        log.info("[示例] 顺序消息消费成功: {}", dto);

        // 业务处理逻辑...
    }

    /**
     * 消费消息入口（供 RocketMQ 5.x gRPC 消费者调用）
     */
    public ConsumeResult consumeMessage(MessageView messageView) {
        return super.consume("TEST_ORDER_TOPIC", "TEST_TAG", messageView);
    }

}
