package com.hc.framework.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.hc.framework.web.exception.GlobalExceptionHandler;
import com.hc.framework.web.serializer.ResultSerializer;
import com.hc.framework.web.wrapper.ResponseWrapAdvice;
import com.hc.framework.web.xss.XssFilter;
import com.hc.framework.web.xss.XssStringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
 * <p>本配置类同时处理 Jackson 消息转换器的注册，确保 WebProperties 能被正确注入。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "hc.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    private final WebProperties webProperties;

    /**
     * 构造器注入 WebProperties，确保配置属性已加载
     */
    public WebAutoConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
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
    @ConditionalOnProperty(prefix = "hc.web", name = "wrap-response", havingValue = "true", matchIfMissing = true)
    public ResponseWrapAdvice responseWrapAdvice() {
        return new ResponseWrapAdvice();
    }

    /**
     * Jackson 消息转换器配置
     *
     * <p>复用 Spring 容器中的 ObjectMapper，在其基础上注册自定义模块，
     * 避免覆盖用户的 spring.jackson.* 配置。</p>
     */
    @Bean
    public WebMvcConfigurer jacksonConfigurer(ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                // 注册自定义模块到已有 ObjectMapper（保留 spring.jackson.* 配置）
                ObjectMapper customized = objectMapper.copy();
                customizeObjectMapper(customized);

                // 创建 Jackson 消息转换器
                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                converter.setObjectMapper(customized);

                // 插入到列表头部，优先使用
                converters.add(0, converter);
            }
        };
    }

    /**
     * 在已有 ObjectMapper 基础上注册自定义模块
     */
    private void customizeObjectMapper(ObjectMapper objectMapper) {
        // 注册自定义模块
        SimpleModule module = new SimpleModule();

        // 注册 Result 序列化器（支持动态字段名）
        module.addSerializer(new ResultSerializer(webProperties, objectMapper));

        // 注册 String 反序列化器（XSS 过滤）
        module.addDeserializer(String.class, new XssStringDeserializer());

        objectMapper.registerModule(module);
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