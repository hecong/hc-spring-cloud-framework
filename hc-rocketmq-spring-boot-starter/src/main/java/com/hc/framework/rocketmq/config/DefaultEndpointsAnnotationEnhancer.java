package com.hc.framework.rocketmq.config;

import org.apache.rocketmq.client.annotation.RocketMQMessageListenerBeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * 自动注入 {@code @RocketMQMessageListener} 的默认值。
 *
 * <p>当注解属性未显式指定时，自动推导：</p>
 * <ul>
 *     <li>{@code endpoints} → 优先从 {@code rocketmq.push-consumer.endpoints} 读取，
 *         其次从 {@code rocketmq.producer.endpoints} 读取</li>
 *     <li>{@code consumerGroup} → 从 {@code spring.application.name} 推导（格式：{appName}_consumer_group）</li>
 *     <li>{@code topic} → 从 {@code spring.application.name} 推导（格式：{appName}-topic）</li>
 * </ul>
 *
 * <p><b>用户显式指定的值始终优先</b>，自动推导仅在注解属性为空字符串或不填时生效。</p>
 *
 * <p><b>注意：</b>注解的默认值是 Spring 占位符（如 {@code ${rocketmq.push-consumer.endpoints:}}），
 * 这些占位符在解析前为非空字符串，会绕过"值为空才注入"的判断逻辑。
 * 因此需要先通过 {@link Environment#resolvePlaceholders} 解析占位符，再判断解析后的值是否为空。</p>
 *
 * <p>可通过以下配置关闭：</p>
 * <ul>
 *     <li>{@code hc.rocketmq.auto-endpoints=false}</li>
 *     <li>{@code hc.rocketmq.auto-consumer-group=false}</li>
 *     <li>{@code hc.rocketmq.auto-topic=false}</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class DefaultEndpointsAnnotationEnhancer
    implements RocketMQMessageListenerBeanPostProcessor.AnnotationEnhancer, EnvironmentAware {

    private Environment environment;
    private String defaultEndpoints;
    private String appName;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        // 优先读取 push-consumer 专用配置，回退到 producer 配置
        this.defaultEndpoints = environment.getProperty("rocketmq.push-consumer.endpoints",
            environment.getProperty("rocketmq.producer.endpoints", ""));
        this.appName = environment.getProperty("spring.application.name", "");
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> attrs, AnnotatedElement element) {
        enhanceEndpoints(attrs);
        enhanceConsumerGroup(attrs);
        enhanceTopic(attrs);
        return attrs;
    }

    private void enhanceEndpoints(Map<String, Object> attrs) {
        String endpoints = resolveIfPresent(attrs.get("endpoints"));
        if (endpoints.isEmpty() && !defaultEndpoints.isEmpty()) {
            attrs.put("endpoints", defaultEndpoints);
        }
    }

    private void enhanceConsumerGroup(Map<String, Object> attrs) {
        String consumerGroup = resolveIfPresent(attrs.get("consumerGroup"));
        if (consumerGroup.isEmpty() && !appName.isEmpty()) {
            attrs.put("consumerGroup", appName.replaceAll("-", "_") + "_consumer_group");
        }
    }

    private void enhanceTopic(Map<String, Object> attrs) {
        String topic = resolveIfPresent(attrs.get("topic"));
        if (topic.isEmpty() && !appName.isEmpty()) {
            attrs.put("topic", appName + "-topic");
        }
    }

    /**
     * 获取属性值并解析其中的 Spring 占位符。
     *
     * <p>注解默认值可能是 {@code ${rocketmq.push-consumer.endpoints:}} 这样的占位符，
     * 在解析前它是非空字符串，会绕过空值判断。此方法先解析占位符再返回实际值。</p>
     *
     * @param value 注解属性原始值
     * @return 解析后的实际值，若为空则返回空字符串
     */
    private String resolveIfPresent(Object value) {
        if (value == null) {
            return "";
        }
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return "";
        }
        // 解析 Spring 占位符，如 ${rocketmq.push-consumer.endpoints:}
        // 若属性未配置，占位符默认值 : 后缀会使其解析为空字符串
        try {
            return environment.resolvePlaceholders(str);
        } catch (Exception e) {
            // 解析失败时返回原始值（可能是不含占位符的用户显式指定值）
            return str;
        }
    }
}
