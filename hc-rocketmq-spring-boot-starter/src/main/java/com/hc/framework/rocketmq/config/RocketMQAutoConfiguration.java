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
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.autoconfigure.RocketMQProperties;
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
@MapperScan("com.hc.framework.rocketmq.mapper")
public class RocketMQAutoConfiguration extends RocketMQBaseConfig {

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
     *
     * <p>注意：不使用 {@code @ConditionalOnBean(MqTransactionLogMapper.class)} 守卫。
     * 原因：本类上的 {@code @MapperScan} 注册的 {@code MapperScannerConfigurer} 是
     * {@code BeanDefinitionRegistryPostProcessor}，在 {@code ConfigurationClassPostProcessor}
     * 之后才执行；而 {@code @ConditionalOnBean} 在配置类解析阶段判定，此时 mapper 尚未注册，
     * 条件恒不满足，会导致本 Bean 及下游 {@link TransactionalMessageSender} 永不创建。
     * {@code @MapperScan} 已保证运行时 mapper 存在，{@code @ConditionalOnClass} 守卫类路径即可。</p>
     */
    @Bean
    @ConditionalOnMissingBean(TransactionLogStore.class)
    @ConditionalOnClass({SqlSessionFactory.class})
    public DefaultTransactionLogStore defaultTransactionLogStore(MqTransactionLogMapper mapper) {
        log.info("[RocketMQ] 配置 DefaultTransactionLogStore（内置事务日志存储已启用）");
        return new DefaultTransactionLogStore(mapper);
    }

    /**
     * 通用事务消息回查 Checker。
     *
     * <p>基于 {@link TransactionLogStore} 判断本地事务是否已提交。</p>
     *
     * <p><b>注意：</b>此 Bean 必须在 {@code rocketMQTransactionTemplate} 之前定义，
     * 否则 {@code @ConditionalOnBean(UniversalTransactionChecker.class)} 无法感知。</p>
     */
    @Bean
    @ConditionalOnMissingBean(UniversalTransactionChecker.class)
    @ConditionalOnBean(TransactionLogStore.class)
    public UniversalTransactionChecker universalTransactionChecker(TransactionLogStore transactionLogStore) {
        log.info("[RocketMQ] 配置 UniversalTransactionChecker（通用事务回查已启用）");
        return new UniversalTransactionChecker(transactionLogStore);
    }

    /**
     * 事务消息专用 Template（绑定 {@link UniversalTransactionChecker} 的 Producer）。
     *
     * <p>与官方 Starter 创建的默认 {@code rocketMQClientTemplate}（普通 Producer）不同，
     * 此 Template 底层的 Producer 已设置事务回查器，才能调用
     * {@code sendTransactionMessage()} 发送事务半消息。</p>
     */
    @Bean
    @ConditionalOnBean(UniversalTransactionChecker.class)
    public RocketMQClientTemplate rocketMQTransactionTemplate(
        ClientServiceProvider clientServiceProvider,
        UniversalTransactionChecker checker,
        RocketMQProperties rocketMQProperties) throws ClientException {
        log.info("[RocketMQ] 配置 rocketMQTransactionTemplate（事务 Producer）");
        Producer producer = clientServiceProvider.newProducerBuilder()
            .setClientConfiguration(buildClientConfig(rocketMQProperties))
            .setTransactionChecker(checker)
            .build();
        return buildTransactionTemplate(producer);
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
        @Qualifier("rocketMQTransactionTemplate") RocketMQClientTemplate template,
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

}
