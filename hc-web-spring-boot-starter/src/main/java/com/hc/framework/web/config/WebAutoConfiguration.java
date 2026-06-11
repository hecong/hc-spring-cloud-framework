package com.hc.framework.web.config;

import com.hc.framework.web.exception.GlobalExceptionHandler;
import com.hc.framework.web.serializer.ResultSerializer;
import com.hc.framework.web.wrapper.ResponseWrapAdvice;
import com.hc.framework.web.xss.XssFilter;
import com.hc.framework.web.xss.XssStringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Web Starter 自动配置类
 *
 * <p>通过 {@link ObjectMapper#rebuild()} 在 Spring 托管的 ObjectMapper 基础上注册自定义 Module，
 * 所有 {@code MappingJackson2HttpMessageConverter} 共享同一个 ObjectMapper。</p>
 *
 * <p>Jackson 3.x 的 ObjectMapper 是不可变的，因此通过 @Primary Bean 覆盖 Boot 默认实例，
 * 确保 ResultSerializer 和 XssStringDeserializer 生效。</p>
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
     * 注册自定义 Jackson Module：ResultSerializer + XssStringDeserializer
     * <p>
     * Jackson 3.x 的 ObjectMapper 不可变，需通过 rebuild() 创建新实例。
     * 使用 @Primary 确保 MVC 的 HttpMessageConverter 使用此定制版本。
     *
     *   自动配置的 ObjectMapper（可能为空，如测试环境）
     */
    @Bean
    @Primary
    public ObjectMapper customObjectMapper(ObjectProvider<ObjectMapper> bootMapperProvider) {
        ObjectMapper base = bootMapperProvider.getIfAvailable(() -> JsonMapper.builder().build());
        SimpleModule module = new SimpleModule();
        module.addSerializer(new ResultSerializer(webProperties));
        module.addDeserializer(String.class, new XssStringDeserializer());
        return base.rebuild()
            .addModule(module)
            .build();
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
}
