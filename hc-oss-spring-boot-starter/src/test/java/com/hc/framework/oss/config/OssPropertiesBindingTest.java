package com.hc.framework.oss.config;

import com.hc.framework.oss.support.OssUploadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hc.oss.upload-validation 配置组绑定单测（2.1 绑定语义）
 */
class OssPropertiesBindingTest {

    private static OssProperties bind(Map<String, String> props) {
        Binder binder = new Binder(new MapConfigurationPropertySource(props));
        BindResult<OssProperties> result =
                binder.bind("hc.oss", Bindable.of(OssProperties.class));
        return result.orElseThrow(() -> new IllegalStateException("hc.oss 配置绑定失败"));
    }

    @Test
    @DisplayName("未配置 upload-validation 时使用默认值：enabled=true、无自定义扩展名、上限 100MB")
    void bindsDefaults() {
        // 需要一个前缀属性以触发 OssProperties 实例化；upload-validation 不提供，走默认值
        OssProperties props = bind(Map.of("hc.oss.default-type", "aliyun"));
        OssProperties.UploadValidationConfig config = props.getUploadValidation();
        assertThat(config).isNotNull();
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getAllowedExtensions()).isNull();
        assertThat(config.getMaxFileSize())
                .isEqualTo(OssUploadValidator.DEFAULT_MAX_FILE_SIZE);
    }

    @Test
    @DisplayName("yaml/自定义前缀：enabled/扩展名列表/大小上限正确绑定并归一化前导点号")
    void bindsCustomValues() {
        OssProperties props = bind(Map.of(
                "hc.oss.upload-validation.enabled", "false",
                "hc.oss.upload-validation.allowed-extensions", ".log,jpg",
                "hc.oss.upload-validation.max-file-size", "2048"
        ));
        OssProperties.UploadValidationConfig config = props.getUploadValidation();
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getAllowedExtensions()).containsExactly(".log", "jpg");
        assertThat(config.getMaxFileSize()).isEqualTo(2048);
    }

    @Test
    @DisplayName("枚举/列表与 default-type 平级字段不互相干扰")
    void coexistsWithProviderConfig() {
        OssProperties props = bind(Map.of(
                "hc.oss.default-type", "minio",
                "hc.oss.upload-validation.enabled", "true"
        ));
        assertThat(props.getDefaultType()).isEqualTo("minio");
        assertThat(props.getUploadValidation().isEnabled()).isTrue();
    }
}
