package com.hc.framework.logging.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hc.logging.apiLog 采样/体积配置组绑定单测
 */
class LoggingPropertiesBindingTest {

    private static LoggingProperties bind(Map<String, String> props) {
        Binder binder = new Binder(new MapConfigurationPropertySource(props));
        return binder.bind("hc.logging", Bindable.of(LoggingProperties.class))
                .orElseThrow(() -> new IllegalStateException("hc.logging 配置绑定失败"));
    }

    @Test
    @DisplayName("apiLog 默认值：sampleRate=1.0、maxSerializeLength=4096")
    void bindsDefaults() {
        LoggingProperties props = bind(Map.of("hc.logging.enabled", "true"));
        LoggingProperties.ApiLogConfig config = props.getApiLog();
        assertThat(config.getSampleRate()).isEqualTo(1.0);
        assertThat(config.getMaxSerializeLength()).isEqualTo(4096);
    }

    @Test
    @DisplayName("自定义采样率与体积上限正确绑定")
    void bindsCustomValues() {
        LoggingProperties props = bind(Map.of(
                "hc.logging.enabled", "true",
                "hc.logging.api-log.sample-rate", "0.25",
                "hc.logging.api-log.max-serialize-length", "512"
        ));
        LoggingProperties.ApiLogConfig config = props.getApiLog();
        assertThat(config.getSampleRate()).isEqualTo(0.25);
        assertThat(config.getMaxSerializeLength()).isEqualTo(512);
    }

    @Test
    @DisplayName("apiLog 与限流/敏感项平级字段互不干扰")
    void coexistsWithOtherGroups() {
        LoggingProperties props = bind(Map.of(
                "hc.logging.sensitive-param-names", "password,idCard",
                "hc.logging.rate-limit.default-qps", "50",
                "hc.logging.api-log.sample-rate", "0.8"
        ));
        assertThat(props.getSensitiveParamNames()).containsExactly("password", "idCard");
        assertThat(props.getRateLimit().getDefaultQps()).isEqualTo(50.0);
        assertThat(props.getApiLog().getSampleRate()).isEqualTo(0.8);
    }
}
