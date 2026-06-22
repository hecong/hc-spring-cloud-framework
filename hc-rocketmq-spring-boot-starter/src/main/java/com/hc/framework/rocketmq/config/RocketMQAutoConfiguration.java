package com.hc.framework.rocketmq.config;

import com.hc.framework.rocketmq.core.sender.BatchMessageSender;
import com.hc.framework.rocketmq.core.sender.DelayMessageSender;
import com.hc.framework.rocketmq.core.sender.FifoMessageSender;
import com.hc.framework.rocketmq.core.sender.NormalMessageSender;
import com.hc.framework.rocketmq.core.sender.RocketMqSender;
import com.hc.framework.rocketmq.core.sender.TransactionalMessageSender;
import com.hc.framework.rocketmq.core.transaction.TransactionLogStore;
import com.hc.framework.rocketmq.core.transaction.UniversalTransactionChecker;
import com.hc.framework.rocketmq.mapper.MqTransactionLogMapper;
import com.hc.framework.rocketmq.service.DefaultTransactionLogStore;
import com.hc.framework.rocketmq.util.IdempotentUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * RocketMQ 自动配置类
 *
 * <p>自动配置 RocketMQ 相关组件：</p>
 * <ul>
 *     <li>独立 Sender：NormalMessageSender / DelayMessageSender / FifoMessageSender / BatchMessageSender</li>
 *     <li>事务消息：TransactionalMessageSender（框架自动管理 DB 事务 + MQ commit/rollback）</li>
 *     <li>门面：RocketMqSender（兼容旧 API，委托给独立 Sender）</li>
 *     <li>幂等：IdempotentUtils（Redis 可用时）</li>
 *     <li>注解增强：DefaultEndpointsAnnotationEnhancer（endpoints/topic/consumerGroup 自动推导）</li>
 *     <li>事务回查：UniversalTransactionChecker（TransactionLogStore 可用时）</li>
 *     <li>事务日志：DefaultTransactionLogStore（MyBatis-Plus 可用时）</li>
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

    // ====================== 配置属性 ======================

    @Value("${hc.rocketmq.producer-logger-enable:true}")
    private boolean producerLoggerEnable;

    // ====================== 独立 Sender（语义分层） ======================

    @Bean
    @ConditionalOnMissingBean(NormalMessageSender.class)
    public NormalMessageSender normalMessageSender(
        @Qualifier("rocketMQClientTemplate") RocketMQClientTemplate template) {
        log.info("[RocketMQ] 配置 NormalMessageSender");
        return new NormalMessageSender(template, producerLoggerEnable);
    }

    @Bean
    @ConditionalOnMissingBean(DelayMessageSender.class)
    public DelayMessageSender delayMessageSender(
        @Qualifier("rocketMQClientTemplate") RocketMQClientTemplate template) {
        log.info("[RocketMQ] 配置 DelayMessageSender");
        return new DelayMessageSender(template, producerLoggerEnable);
    }

    @Bean
    @ConditionalOnMissingBean(FifoMessageSender.class)
    public FifoMessageSender fifoMessageSender(
        @Qualifier("rocketMQClientTemplate") RocketMQClientTemplate template) {
        log.info("[RocketMQ] 配置 FifoMessageSender");
        return new FifoMessageSender(template, producerLoggerEnable);
    }

    @Bean
    @ConditionalOnMissingBean(BatchMessageSender.class)
    public BatchMessageSender batchMessageSender(NormalMessageSender normalSender) {
        log.info("[RocketMQ] 配置 BatchMessageSender");
        return new BatchMessageSender(normalSender, producerLoggerEnable);
    }

    // ====================== 事务消息（框架自动管理事务） ======================

    /**
     * 内置事务日志存储（仅 MyBatis-Plus 可用时激活）。
     * 业务方实现 TransactionLogStore 并注册 Bean 后，此默认实现自动跳过。
     */
    @Bean
    @ConditionalOnMissingBean(TransactionLogStore.class)
    @ConditionalOnClass({SqlSessionFactory.class})
    @ConditionalOnBean(MqTransactionLogMapper.class)
    public DefaultTransactionLogStore defaultTransactionLogStore(MqTransactionLogMapper mapper) {
        log.info("[RocketMQ] 配置 DefaultTransactionLogStore（内置事务日志存储已启用）");
        return new DefaultTransactionLogStore(mapper);
    }

    /**
     * 事务消息发送器（框架管理 DB 事务 + MQ commit/rollback）。
     * 需要 TransactionTemplate 和 TransactionLogStore 同时可用。
     */
    @Bean
    @ConditionalOnMissingBean(TransactionalMessageSender.class)
    @ConditionalOnClass(TransactionTemplate.class)
    @ConditionalOnBean(TransactionLogStore.class)
    public TransactionalMessageSender transactionalMessageSender(
        @Qualifier("rocketMQClientTemplate") RocketMQClientTemplate template,
        TransactionLogStore transactionLogStore,
        TransactionTemplate transactionTemplate) {
        log.info("[RocketMQ] 配置 TransactionalMessageSender（事务自动管理已启用）");
        return new TransactionalMessageSender(template, transactionLogStore, transactionTemplate,
            producerLoggerEnable);
    }

    // ====================== 门面（兼容旧 API） ======================

    @Bean
    @ConditionalOnMissingBean(RocketMqSender.class)
    public RocketMqSender rocketMqSender(
        NormalMessageSender normalSender,
        DelayMessageSender delaySender,
        FifoMessageSender fifoSender,
        BatchMessageSender batchSender,
        @Lazy TransactionalMessageSender transactionalSender,
        @Lazy Map<String, RocketMQClientTemplate> templateMap) {
        log.info("[RocketMQ] 配置 RocketMqSender（门面模式，委托到独立 Sender）");
        return new RocketMqSender(normalSender, delaySender, fifoSender, batchSender,
            transactionalSender, templateMap);
    }

    // ====================== 幂等 ======================

    @Bean
    @ConditionalOnMissingBean(IdempotentUtils.class)
    @ConditionalOnBean(org.springframework.data.redis.core.RedisTemplate.class)
    public IdempotentUtils idempotentUtils() {
        log.info("[RocketMQ] 配置 IdempotentUtils（Redis 幂等）");
        return new IdempotentUtils();
    }

    // ====================== 注解增强（endpoints/topic/consumerGroup 自动推导） ======================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.rocketmq", name = "auto-endpoints",
        havingValue = "true", matchIfMissing = true)
    public DefaultEndpointsAnnotationEnhancer defaultEndpointsAnnotationEnhancer(Environment environment) {
        DefaultEndpointsAnnotationEnhancer enhancer = new DefaultEndpointsAnnotationEnhancer();
        enhancer.setEnvironment(environment);
        log.info("[RocketMQ] 配置 DefaultEndpointsAnnotationEnhancer（endpoints/topic/consumerGroup 自动推导已启用）");
        return enhancer;
    }

    // ====================== 事务回查 ======================

    @Bean
    @ConditionalOnMissingBean(UniversalTransactionChecker.class)
    @ConditionalOnBean(TransactionLogStore.class)
    public UniversalTransactionChecker universalTransactionChecker(TransactionLogStore transactionLogStore) {
        log.info("[RocketMQ] 配置 UniversalTransactionChecker（通用事务回查已启用）");
        return new UniversalTransactionChecker(transactionLogStore);
    }

    // ====================== MyBatis-Plus Mapper 扫描 ======================

    @Configuration
    @ConditionalOnClass({SqlSessionFactory.class})
    @MapperScan(basePackages = {"com.hc.framework.rocketmq.mapper"})
    public static class RocketMQMapperConfiguration {
    }
}
