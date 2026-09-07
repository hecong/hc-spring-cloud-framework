package com.hc.framework.web.config;

import com.hc.framework.common.util.IpUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hc.web.trusted-proxies → IpUtils 静态持有者注入（启动装配）
 */
class WebTrustedProxiesContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        IpUtils.setTrustedProxies(Collections.emptyList());
    }

    @Test
    @DisplayName("配置 trusted-proxies：初始化器注入并驱动 getClientIp 可信链解析")
    void trustedProxiesConfiguredAndInjected() {
        runner.withPropertyValues("hc.web.trusted-proxies=10.0.0.0/8,192.168.1.1")
                .run(context -> {
                    assertThat(context).hasSingleBean(IpTrustedProxiesInitializer.class);

                    MockHttpServletRequest request = new MockHttpServletRequest();
                    request.setRemoteAddr("10.0.0.5");
                    request.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.4, 10.0.0.5");
                    assertThat(IpUtils.getClientIp(request)).isEqualTo("8.8.8.8");
                });
    }

    @Test
    @DisplayName("不配置 trusted-proxies：保持安全默认，伪造 XFF 无效")
    void emptyDefaultStaysSafe() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(IpTrustedProxiesInitializer.class);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("8.8.8.8");
            request.addHeader("X-Forwarded-For", "1.2.3.4");
            assertThat(IpUtils.getClientIp(request)).isEqualTo("8.8.8.8");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomInitializerConfig {
        @Bean
        IpTrustedProxiesInitializer ipTrustedProxiesInitializer() {
            return new IpTrustedProxiesInitializer(List.of("10.0.0.0/8"));
        }
    }

    @Test
    @DisplayName("业务自定义初始化器时默认 Bean 让位")
    void customInitializerTakesPrecedence() {
        new ApplicationContextRunner()
                .withUserConfiguration(CustomInitializerConfig.class)
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(IpTrustedProxiesInitializer.class);
                    MockHttpServletRequest request = new MockHttpServletRequest();
                    request.setRemoteAddr("10.0.0.5");
                    request.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.4");
                    assertThat(IpUtils.getClientIp(request)).isEqualTo("9.9.9.9");
                });
    }
}
