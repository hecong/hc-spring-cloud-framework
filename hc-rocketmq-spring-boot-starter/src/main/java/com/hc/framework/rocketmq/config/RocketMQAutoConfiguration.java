package com.hc.framework.rocketmq.config;

import com.hc.framework.rocketmq.core.RocketMqSender;
import com.hc.framework.rocketmq.util.IdempotentUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 自动配置类
 *
 * <p>自动配置 RocketMQ 相关组件：</p>
 * <ul>
 *     <li>RocketMqSender: 消息发送器</li>
 *     <li>IdempotentUtils: 幂等工具类（当 Redis 存在时）</li>
 * </ul>
 *
 * @author hc-framework
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RocketMQClientTemplate.class)
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQAutoConfiguration {

    public RocketMQAutoConfiguration() {
        log.info("[RocketMQ] 自动配置已加载");
    }

    /**
     * 配置 RocketMqSender
     *
     * @param rocketMQClientTemplate RocketMQ 客户端模板
     * @return RocketMqSender
     */
    @Bean
    @ConditionalOnMissingBean(RocketMqSender.class)
    public RocketMqSender rocketMqSender(RocketMQClientTemplate rocketMQClientTemplate) {
        log.info("[RocketMQ] 配置 RocketMqSender");
        return new RocketMqSender(rocketMQClientTemplate);
    }

    /**
     * 配置 IdempotentUtils（当 RedisTemplate 存在时）
     *
     * @return IdempotentUtils
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentUtils.class)
    @ConditionalOnBean(org.springframework.data.redis.core.RedisTemplate.class)
    public IdempotentUtils idempotentUtils() {
        log.info("[RocketMQ] 配置 IdempotentUtils（Redis 幂等）");
        return new IdempotentUtils();
    }

}
