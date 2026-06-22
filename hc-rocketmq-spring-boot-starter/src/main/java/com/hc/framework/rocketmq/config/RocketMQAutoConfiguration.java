package com.hc.framework.rocketmq.config;

import com.hc.framework.rocketmq.core.RocketMqSender;
import com.hc.framework.rocketmq.core.TransactionLogStore;
import com.hc.framework.rocketmq.core.UniversalTransactionChecker;
import com.hc.framework.rocketmq.util.IdempotentUtils;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

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
@ConditionalOnProperty(prefix = "hc.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    public RocketMqSender rocketMqSender(@Qualifier("rocketMQClientTemplate") RocketMQClientTemplate rocketMQClientTemplate,
                                          @Lazy Map<String, RocketMQClientTemplate> templateMap) {
        log.info("[RocketMQ] 配置 RocketMqSender（多 Template 支持已启用）");
        return new RocketMqSender(rocketMQClientTemplate, templateMap);
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

    /**
     * 配置自动 endpoints 增强器（默认启用）。
     *
     * <p>当 {@code @RocketMQMessageListener} 未显式指定 endpoints 时，
     * 自动注入 {@code rocketmq.producer.endpoints}。</p>
     *
     * <p>通过 {@code hc.rocketmq.auto-endpoints=false} 关闭。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.rocketmq", name = "auto-endpoints",
                           havingValue = "true", matchIfMissing = true)
    public DefaultEndpointsAnnotationEnhancer defaultEndpointsAnnotationEnhancer(Environment environment) {
        DefaultEndpointsAnnotationEnhancer enhancer = new DefaultEndpointsAnnotationEnhancer();
        enhancer.setEnvironment(environment);
        log.info("[RocketMQ] 配置 DefaultEndpointsAnnotationEnhancer（自动 endpoints 已启用）");
        return enhancer;
    }

    /**
     * 配置通用事务消息回查 Checker（当存在 TransactionLogStore 时）。
     *
     * <p>使用默认 Template {@code rocketMQClientTemplate}，所有业务共享此 Checker。
     * 引入 TransactionLogStore Bean 后自动生效。</p>
     */
    @Bean
    @ConditionalOnMissingBean(UniversalTransactionChecker.class)
    @ConditionalOnBean(TransactionLogStore.class)
    public UniversalTransactionChecker universalTransactionChecker(TransactionLogStore transactionLogStore) {
        log.info("[RocketMQ] 配置 UniversalTransactionChecker（通用事务回查已启用）");
        return new UniversalTransactionChecker(transactionLogStore);
    }

}
