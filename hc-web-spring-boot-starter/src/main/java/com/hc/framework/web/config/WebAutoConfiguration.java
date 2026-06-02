package com.hc.framework.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hc.framework.web.exception.GlobalExceptionHandler;
import com.hc.framework.web.serializer.ResultSerializer;
import com.hc.framework.web.wrapper.ResponseWrapAdvice;
import com.hc.framework.web.xss.XssFilter;
import com.hc.framework.web.xss.XssStringDeserializer;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Web Starter 自动配置类
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>hc.web.enabled: 是否启用 Web Starter 功能（默认 true）</li>
 *   <li>hc.web.wrap-response: 是否包装响应结果为 Result（默认 true）</li>
 *   <li>hc.web.code-field: 响应码字段名（默认 code）</li>
 *   <li>hc.web.message-field: 响应消息字段名（默认 message）</li>
 *   <li>hc.web.data-field: 响应数据字段名（默认 data）</li>
 * </ul>
 *
 * <p>通过 {@link PostConstruct} 在 Spring 托管的 ObjectMapper 上直接注册自定义 Module，
 * 所有 {@code MappingJackson2HttpMessageConverter} 共享同一个 ObjectMapper，
 * 无需 copy 或额外 Converter。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "hc.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    private final WebProperties webProperties;
    private final ObjectMapper objectMapper;

    /**
     * 构造器注入 WebProperties 与 ObjectMapper
     */
    public WebAutoConfiguration(WebProperties webProperties, ObjectMapper objectMapper) {
        this.webProperties = webProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 在 Spring 托管的 ObjectMapper 上注册自定义 Module
     * <p>
     * 所有 Converter 共享同一 ObjectMapper 实例，无需 copy。
     * ResultSerializer 仅匹配 Result 类型，XssStringDeserializer 仅匹配 String 类型，
     * 不会影响其他类型的序列化/反序列化。
     */
    @PostConstruct
    public void customizeObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(new ResultSerializer(webProperties, objectMapper));
        module.addDeserializer(String.class, new XssStringDeserializer());
        objectMapper.registerModule(module);
    }

    /**
     * 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 响应包装器（实现 wrapResponse 开关）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.web", name = "wrap-response", havingValue = "true", matchIfMissing = true)
    public ResponseWrapAdvice responseWrapAdvice() {
        return new ResponseWrapAdvice(webProperties);
    }

    /**
     * XSS 过滤器注册
     *
     * <p>对 HTTP 请求参数进行 XSS 过滤，防止恶意脚本注入。</p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "hc.web", name = "xss-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilter() {
        FilterRegistrationBean<XssFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new XssFilter(webProperties));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
