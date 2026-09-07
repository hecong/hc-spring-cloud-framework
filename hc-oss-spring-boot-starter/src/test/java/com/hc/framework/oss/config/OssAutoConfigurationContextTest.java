package com.hc.framework.oss.config;

import com.hc.framework.oss.service.OssService;
import com.hc.framework.oss.service.impl.MinioOssServiceImpl;
import com.hc.framework.oss.support.OssUploadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配上下文测试（2.2 两个开关分支均能创建服务，且校验器配置正确注入）
 */
class OssAutoConfigurationContextTest {

    private static final String[] BASE_PROPS = {
            "hc.oss.default-type=minio",
            "hc.oss.minio.endpoint=http://127.0.0.1:9000",
            "hc.oss.minio.access-key=minioadmin",
            "hc.oss.minio.secret-key=minioadmin",
            "hc.oss.minio.bucket-name=hc-test-bucket"
    };

    private ApplicationContextRunner runner(String... extraProps) {
        String[] props = Arrays.copyOf(BASE_PROPS, BASE_PROPS.length + extraProps.length);
        System.arraycopy(extraProps, 0, props, BASE_PROPS.length, extraProps.length);
        return new ApplicationContextRunner()
                .withPropertyValues(props)
                .withConfiguration(AutoConfigurations.of(OssAutoConfiguration.class));
    }

    @Test
    @DisplayName("校验开启（默认）：校验器 Bean 生效并注入服务实现")
    void validationEnabledCreatesValidatorAndService() {
        runner().run(context -> {
            assertThat(context).hasSingleBean(OssService.class);
            assertThat(context.getBean(OssService.class)).isInstanceOf(MinioOssServiceImpl.class);
            assertThat(context).hasSingleBean(OssUploadValidator.class);
            OssUploadValidator validator = context.getBean(OssUploadValidator.class);
            assertThat(validator.isEnabled()).isTrue();
            assertThat(validator.getMaxFileSize()).isEqualTo(OssUploadValidator.DEFAULT_MAX_FILE_SIZE);
        });
    }

    @Test
    @DisplayName("upload-validation.enabled=false：服务仍创建，但校验器跳过校验")
    void validationDisabledStillCreatesServiceWithDisabledValidator() {
        runner("hc.oss.upload-validation.enabled=false").run(context -> {
            assertThat(context).hasSingleBean(OssService.class);
            assertThat(context).hasSingleBean(OssUploadValidator.class);
            assertThat(context.getBean(OssUploadValidator.class).isEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("自定义白名单与大小上限经自动装配正确透传")
    void customUploadValidationPropagates() {
        runner(
                "hc.oss.upload-validation.enabled=true",
                "hc.oss.upload-validation.allowed-extensions=log,jar",
                "hc.oss.upload-validation.max-file-size=2048"
        ).run(context -> {
            OssUploadValidator validator = context.getBean(OssUploadValidator.class);
            assertThat(validator.isEnabled()).isTrue();
            assertThat(validator.getMaxFileSize()).isEqualTo(2048);
            assertThat(validator.getAllowedExtensions()).containsExactlyInAnyOrder("log", "jar");
        });
    }
}
