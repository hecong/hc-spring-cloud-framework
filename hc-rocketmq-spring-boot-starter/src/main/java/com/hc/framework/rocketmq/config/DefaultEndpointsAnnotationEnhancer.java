package com.hc.framework.rocketmq.config;

import org.apache.rocketmq.client.annotation.RocketMQMessageListenerBeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * 自动注入 push consumer {@code endpoints} 默认值。
 *
 * <p>通过实现 RocketMQ 官方的 {@link RocketMQMessageListenerBeanPostProcessor.AnnotationEnhancer}
 * 接口，在 listener 注解处理前将空白的 {@code endpoints} 自动填充为
 * {@code rocketmq.producer.endpoints} 的值。</p>
 *
 * <p><b>机制：</b></p>
 * <ul>
 *     <li>仅在 {@code @RocketMQMessageListener} 的 {@code endpoints} 为空或不配置时生效</li>
 *     <li>用户显式指定的 {@code endpoints} 优先</li>
 *     <li>通过 {@link EnvironmentAware} 直接从 {@link Environment} 读取配置，
 *         避免依赖 {@code RocketMQProperties} bean 的初始化时序问题</li>
 * </ul>
 *
 * <p>可通过 {@code hc.rocketmq.auto-endpoints=false} 关闭此特性。</p>
 *
 * @author hc-framework
 */
public class DefaultEndpointsAnnotationEnhancer
        implements RocketMQMessageListenerBeanPostProcessor.AnnotationEnhancer, EnvironmentAware {

    private String defaultEndpoints;

    @Override
    public void setEnvironment(Environment environment) {
        this.defaultEndpoints = environment.getProperty("rocketmq.producer.endpoints", "");
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> attrs, AnnotatedElement element) {
        Object endpoints = attrs.get("endpoints");
        if ((endpoints == null || endpoints.toString().trim().isEmpty())
                && !defaultEndpoints.isEmpty()) {
            attrs.put("endpoints", defaultEndpoints);
        }
        return attrs;
    }
}
