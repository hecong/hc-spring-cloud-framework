package com.hc.framework.web.config;

import com.hc.framework.web.exception.GlobalExceptionHandler;
import com.hc.framework.web.serializer.ResultSerializer;
import com.hc.framework.web.wrapper.ResponseWrapAdvice;
import com.hc.framework.web.xss.XssFilter;
import com.hc.framework.web.xss.XssStringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tools.jackson.databind.module.SimpleModule;

/**
 * Web Starter 自动配置类
 *
 * <p>通过 {@link JsonMapperBuilderCustomizer} 在 Spring Boot 构建 JsonMapper 时注入自定义 Module，
 * 所有 {@code MappingJackson2HttpMessageConverter} 共享同一个 ObjectMapper。</p>
 *
 * <p>Jackson 3.x 的 ObjectMapper 是不可变的，因此通过 Builder 层面定制，
 * 而非创建 @Primary 覆盖 —— 避免与 Boot 自身的 jacksonJsonMapper 冲突。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "hc.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    private final WebProperties webProperties;

    public WebAutoConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     * 通过 {@link JsonMapperBuilderCustomizer} 向 Boot 的 JsonMapper.Builder 注册
     * ResultSerializer 和 XssStringDeserializer，确保定制模块融入统一的 ObjectMapper。
     */
    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(new ResultSerializer(webProperties));
            module.addDeserializer(String.class, new XssStringDeserializer());
            builder.addModule(module);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.web", name = "wrap-response", havingValue = "true", matchIfMissing = true)
    public ResponseWrapAdvice responseWrapAdvice() {
        return new ResponseWrapAdvice(webProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "hc.web", name = "xss-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilter() {
        FilterRegistrationBean<XssFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new XssFilter(webProperties));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /**
     * 将 hc.web.trusted-proxies 注入 IpUtils（业务可自定义同名 Bean 覆盖）
     */
    @Bean
    @ConditionalOnMissingBean
    public IpTrustedProxiesInitializer ipTrustedProxiesInitializer() {
        return new IpTrustedProxiesInitializer(webProperties.getTrustedProxies());
    }
}
