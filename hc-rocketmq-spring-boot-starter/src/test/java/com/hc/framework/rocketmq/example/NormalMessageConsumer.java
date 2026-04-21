package com.hc.framework.rocketmq.example;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.core.BaseMqConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 普通消息消费者示例
 *
 * <p>注意：RocketMQ 5.x gRPC 版本的消费者注解路径不同，请使用官方配置方式</p>
 *
 * @author hc-framework
 */
@Slf4j
@Component
// RocketMQ 5.x gRPC 版本的消费者配置方式请参考官方文档
// @RocketMQMessageListener(
//         topic = "TEST_NORMAL_TOPIC",
//         tag = "TEST_TAG",
//         consumerGroup = "GID_TEST_NORMAL"
// )
public class NormalMessageConsumer extends BaseMqConsumer<TestMessageDTO> {

    @Override
    protected void doConsume(TestMessageDTO message) {

        log.info("[示例] 普通消息消费成功: {}", JsonUtils.toJson(message));

        // 业务处理逻辑...
        // 业务上也需要实现幂等性处理   防止控制台重放消息
    }


    @Override
    protected Class<TestMessageDTO> getDataType() {
        return TestMessageDTO.class;
    }

}
