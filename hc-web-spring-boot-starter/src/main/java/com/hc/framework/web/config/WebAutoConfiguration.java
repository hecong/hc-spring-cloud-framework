package com.hc.framework.web.config;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.hc.framework.web.exception.GlobalExceptionHandler;
import com.hc.framework.web.model.Result;
import com.hc.framework.web.serializer.ResultObjectWriter;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
 * <p>本配置类同时处理 Fastjson2 消息转换器的注册，确保 WebProperties 能被正确注入。</p>
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
     * Fastjson2 消息转换器配置
     *
     * <p>使用匿名内部类实现 WebMvcConfigurer，确保在 WebAutoConfiguration 之后加载，
     * 从而能正确获取到 WebProperties。</p>
     */
    @Bean
    public WebMvcConfigurer fastjson2Configurer() {
        // 注册 Result 序列化器（在 Bean 创建时立即注册）
        registerResultSerializer();
        // 注册 XSS String 反序列化器
        registerXssDeserializer();

        return new WebMvcConfigurer() {
            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                // 创建 Fastjson2 消息转换器
                FastJsonHttpMessageConverter converter = createFastJsonConverter();
                // 插入到列表头部，优先使用
                converters.add(0, converter);
            }
        };
    }

    /**
     * 创建并配置 Fastjson2 HttpMessageConverter
     */
    private FastJsonHttpMessageConverter createFastJsonConverter() {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // 配置 Fastjson2 序列化特性
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);

        // 设置序列化特性
        fastJsonConfig.setWriterFeatures(
            JSONWriter.Feature.WriteNulls,           // 输出 null 值
            JSONWriter.Feature.PrettyFormat,         // 格式化输出（可选）
            JSONWriter.Feature.WriteMapNullValue,    // Map 中 null 值也输出
            JSONWriter.Feature.BrowserCompatible     // 浏览器兼容（处理特殊字符）
        );

        converter.setFastJsonConfig(fastJsonConfig);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

        return converter;
    }

    /**
     * 注册 Result 类的自定义序列化器
     */
    private void registerResultSerializer() {
        ResultObjectWriter writer = new ResultObjectWriter(webProperties);
        JSONFactory.getDefaultObjectWriterProvider().register(Result.class, writer);
    }

    /**
     * 注册 XSS String 反序列化器
     *
     * <p>在 Bean 创建时注册到 Fastjson2，对所有 String 类型进行 XSS 过滤。</p>
     */
    private void registerXssDeserializer() {
        JSONFactory.getDefaultObjectReaderProvider().register(String.class, XssStringDeserializer.INSTANCE);
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