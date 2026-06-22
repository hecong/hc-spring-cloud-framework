package com.hc.framework.rocketmq.config;

import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentials;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * RocketMQ 配置基类 —— 提供 {@link ClientServiceProvider} 和可复用的
 * {@link #buildClientConfig(RocketMQProperties)} 模板方法。
 *
 * <p>用于业务侧通过继承来配置多套独立事务 Producer / Template。</p>
 *
 * <p><b>推荐用法</b>（使用 {@link #buildTransactionTemplate} helper，每场景仅需两个 1 行 @Bean 方法）：</p>
 * <pre>{@code
 * @Configuration
 * public class RocketMQTransactionConfig extends RocketMQBaseConfig {
 *
 *     @Bean(name = "orderPayTransProducer", destroyMethod = "close")
 *     public Producer orderPayTransProducer(ClientServiceProvider provider,
 *                                           OrderPayChecker checker,
 *                                           RocketMQProperties props) throws ClientException {
 *         return buildTransactionProducer(provider, checker, props, "OrderTopic");
 *     }
 *
 *     @Bean
 *     public RocketMQClientTemplate orderPayTransTemplate(
 *             @Qualifier("orderPayTransProducer") Producer producer) {
 *         return buildTransactionTemplate(producer);
 *     }
 * }
 * }</pre>
 *
 * <p>注意：本类不标注 {@code @Configuration}，仅作为一个模板基类。业务子类标注
 * {@code @Configuration} 后，Spring 会自动发现父类中的 {@code @Bean} 方法。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class RocketMQBaseConfig {


    /**
     * 创建 RocketMQ 5.x gRPC 客户端服务入口。
     *
     * <p>{@link ConditionalOnMissingBean} 确保在官方 Starter 已提供时不会重复创建。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientServiceProvider clientServiceProvider() {
        return ClientServiceProvider.loadService();
    }

    /**
     * 构建 {@link ClientConfiguration}（从 {@link RocketMQProperties} 读取配置）。
     *
     * <p>每次调用均创建新实例，可安全用于多个 Producer/Consumer。</p>
     *
     * @return ClientConfiguration
     * @throws IllegalStateException 如果 Producer 配置未设置
     */
    public ClientConfiguration buildClientConfig(RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer prop = rocketMQProperties.getProducer();
        if (prop == null) {
            throw new IllegalStateException(
                "RocketMQ Producer 未配置，请在 application.yml 中设置 rocketmq.producer.*");
        }

        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
            .setEndpoints(prop.getEndpoints())
            .setRequestTimeout(Duration.ofSeconds(prop.getRequestTimeout()));

        if (prop.getAccessKey() != null && !prop.getAccessKey().isBlank()) {
            builder.setCredentialProvider(
                () -> new SessionCredentials(prop.getAccessKey(), prop.getSecretKey()));
        }
        if (prop.getNamespace() != null && !prop.getNamespace().isBlank()) {
            builder.setNamespace(prop.getNamespace());
        }
        return builder.build();
    }

    /**
     * 构建事务 Producer（绑定指定 Checker 和 Topics）。
     *
     * <p>用于多事务场景，每个事务场景应创建独立的 Producer 实例。</p>
     *
     * @param clientServiceProvider 客户端服务入口（继承自基类的 Bean）
     * @param checker               事务回查逻辑
     * @param rocketMQProperties    RocketMQ 配置属性
     * @param topics                该 Producer 订阅的 Topic 列表
     * @return 已构建的 Producer 实例
     * @throws org.apache.rocketmq.client.apis.ClientException Producer 构建失败
     */
    public Producer buildTransactionProducer(ClientServiceProvider clientServiceProvider,
                                             TransactionChecker checker,
                                             RocketMQProperties rocketMQProperties,
                                             String... topics) throws org.apache.rocketmq.client.apis.ClientException {
        return clientServiceProvider.newProducerBuilder()
                .setClientConfiguration(buildClientConfig(rocketMQProperties))
                .setTopics(topics)
                .setTransactionChecker(checker)
                .build();
    }

    /**
     * 构建事务 Template（持有指定 Producer）。
     *
     * <p>返回的 Template 的 beanName 由调用方的 {@code @Bean} 方法名决定，
     * 需与 Checker 上 {@code @RocketMQTransactionListener(rocketMQTemplateBeanName = "xxx")} 的值一致。</p>
     *
     * @param producer 事务 Producer
     * @return 已配置 Producer 的 RocketMQClientTemplate
     */
    public RocketMQClientTemplate buildTransactionTemplate(Producer producer) {
        RocketMQClientTemplate template = new RocketMQClientTemplate();
        template.setProducer(producer);
        return template;
    }
}