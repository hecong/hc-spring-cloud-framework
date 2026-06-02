package com.hc.framework.oss.config;

import com.hc.framework.oss.service.OssService;
import com.hc.framework.oss.service.impl.AliyunOssServiceImpl;
import com.hc.framework.oss.service.impl.MinioOssServiceImpl;
import com.hc.framework.oss.service.impl.TencentCosServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OSS Starter 自动配置类
 * <p>
 * 根据 {@code hc.oss.default-type} 创建对应的云存储服务实现，
 * 可选值：aliyun、minio、tencent-cos，不允许混用。
 * <p>
 * 启动时校验所选 provider 的必填配置项，缺失则 fast-fail 阻止启动。
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "hc.oss", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OssAutoConfiguration {

    public OssAutoConfiguration(OssProperties ossProperties) {
        validateSelectedProvider(ossProperties);
    }

    /**
     * 根据 defaultType 创建唯一的 OSS 服务实现
     */
    @Bean
    @ConditionalOnMissingBean
    public OssService ossService(OssProperties ossProperties) {
        String type = ossProperties.getDefaultType();
        log.info("创建OSS服务实现: type={}", type);
        return switch (type) {
            case "aliyun" -> new AliyunOssServiceImpl(ossProperties.getAliyun());
            case "minio" -> new MinioOssServiceImpl(ossProperties.getMinio());
            case "tencent-cos" -> new TencentCosServiceImpl(ossProperties.getTencentCos());
            default -> throw new IllegalStateException(
                    "不支持的OSS存储类型: " + type + "，可选值: aliyun, minio, tencent-cos");
        };
    }

    /**
     * 启动时校验所选 provider 的必要配置
     */
    private void validateSelectedProvider(OssProperties properties) {
        String type = properties.getDefaultType();
        if (type == null || type.isBlank()) {
            throw new IllegalStateException(
                    "hc.oss.default-type 不能为空，可选值: aliyun, minio, tencent-cos");
        }
        switch (type) {
            case "aliyun" -> validateAliyun(properties.getAliyun());
            case "minio" -> validateMinio(properties.getMinio());
            case "tencent-cos" -> validateTencentCos(properties.getTencentCos());
            default ->
                    throw new IllegalStateException(
                            "不支持的OSS存储类型: " + type + "，可选值: aliyun, minio, tencent-cos");
        }
    }

    private void validateAliyun(OssProperties.AliyunOssConfig config) {
        if (isBlank(config.getEndpoint())) {
            throw new IllegalStateException("阿里云OSS endpoint 未配置 (hc.oss.aliyun.endpoint)");
        }
        if (isBlank(config.getAccessKeyId())) {
            throw new IllegalStateException("阿里云OSS accessKeyId 未配置 (hc.oss.aliyun.access-key-id)");
        }
        if (isBlank(config.getAccessKeySecret())) {
            throw new IllegalStateException("阿里云OSS accessKeySecret 未配置 (hc.oss.aliyun.access-key-secret)");
        }
        if (isBlank(config.getBucketName())) {
            throw new IllegalStateException("阿里云OSS bucketName 未配置 (hc.oss.aliyun.bucket-name)");
        }
    }

    private void validateMinio(OssProperties.MinioConfig config) {
        if (isBlank(config.getEndpoint())) {
            throw new IllegalStateException("MinIO endpoint 未配置 (hc.oss.minio.endpoint)");
        }
        if (isBlank(config.getAccessKey())) {
            throw new IllegalStateException("MinIO accessKey 未配置 (hc.oss.minio.access-key)");
        }
        if (isBlank(config.getSecretKey())) {
            throw new IllegalStateException("MinIO secretKey 未配置 (hc.oss.minio.secret-key)");
        }
        if (isBlank(config.getBucketName())) {
            throw new IllegalStateException("MinIO bucketName 未配置 (hc.oss.minio.bucket-name)");
        }
    }

    private void validateTencentCos(OssProperties.TencentCosConfig config) {
        if (isBlank(config.getRegion())) {
            throw new IllegalStateException("腾讯云COS region 未配置 (hc.oss.tencent-cos.region)");
        }
        if (isBlank(config.getSecretId())) {
            throw new IllegalStateException("腾讯云COS secretId 未配置 (hc.oss.tencent-cos.secret-id)");
        }
        if (isBlank(config.getSecretKey())) {
            throw new IllegalStateException("腾讯云COS secretKey 未配置 (hc.oss.tencent-cos.secret-key)");
        }
        if (isBlank(config.getBucketName())) {
            throw new IllegalStateException("腾讯云COS bucketName 未配置 (hc.oss.tencent-cos.bucket-name)");
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
