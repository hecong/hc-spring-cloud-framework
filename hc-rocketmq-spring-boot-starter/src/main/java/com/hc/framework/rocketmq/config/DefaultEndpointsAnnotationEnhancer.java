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
 *     <li>{@code endpoints} → 从 {@code rocketmq.producer.endpoints} 读取</li>
 *     <li>{@code consumerGroup} → 从 {@code spring.application.name} 推导（格式：{appName}_consumer_group）</li>
 *     <li>{@code topic} → 从 {@code spring.application.name} 推导（格式：{appName}-topic）</li>
 * </ul>
 *
 * <p><b>用户显式指定的值始终优先</b>，自动推导仅在注解属性为空字符串或不填时生效。</p>
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

    private String defaultEndpoints;
    private String appName;

    @Override
    public void setEnvironment(Environment environment) {
        this.defaultEndpoints = environment.getProperty("rocketmq.producer.endpoints", "");
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
        Object endpoints = attrs.get("endpoints");
        if ((endpoints == null || endpoints.toString().trim().isEmpty()) && !defaultEndpoints.isEmpty()) {
            attrs.put("endpoints", defaultEndpoints);
        }
    }

    private void enhanceConsumerGroup(Map<String, Object> attrs) {
        Object consumerGroup = attrs.get("consumerGroup");
        if ((consumerGroup == null || consumerGroup.toString().trim().isEmpty()) && !appName.isEmpty()) {
            attrs.put("consumerGroup", appName.replaceAll("-", "_") + "_consumer_group");
        }
    }

    private void enhanceTopic(Map<String, Object> attrs) {
        Object topic = attrs.get("topic");
        if ((topic == null || topic.toString().trim().isEmpty()) && !appName.isEmpty()) {
            attrs.put("topic", appName + "-topic");
        }
    }
}
