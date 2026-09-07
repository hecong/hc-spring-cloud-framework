package com.hc.framework.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * OSS 配置属性
 * <p>
 * 通过 {@code hc.oss.default-type} 指定使用的云存储服务（单选，不允许混用）。
 * 可选值：{@code aliyun}、{@code minio}、{@code tencent-cos}。
 */
@Data
@ConfigurationProperties(prefix = "hc.oss")
public class OssProperties {

    /**
     * 是否启用 OSS 模块
     */
    private Boolean enabled = true;

    /**
     * 默认存储类型，可选值：aliyun、minio、tencent-cos
     */
    private String defaultType = "aliyun";

    /**
     * 阿里云 OSS 配置
     */
    private AliyunOssConfig aliyun = new AliyunOssConfig();

    /**
     * MinIO 配置
     */
    private MinioConfig minio = new MinioConfig();

    /**
     * 腾讯云 COS 配置
     */
    private TencentCosConfig tencentCos = new TencentCosConfig();

    /**
     * 上传校验配置（默认开启，BREAKING：升级后框架会按白名单校验并按需关闭流）
     */
    private UploadValidationConfig uploadValidation = new UploadValidationConfig();

    /**
     * 上传校验配置
     */
    @Data
    public static class UploadValidationConfig {

        /**
         * 是否启用上传校验（扩展名/魔数/大小），默认 true；false 仅关闭校验，流仍由框架关闭
         */
        private boolean enabled = true;

        /**
         * 允许的扩展名白名单（自动转小写、容忍前导点号）；为空时使用内置默认表
         */
        private List<String> allowedExtensions;

        /**
         * 单文件大小上限（字节），默认 100MB（104857600）
         */
        private long maxFileSize = 100L * 1024 * 1024;
    }

    /**
     * 阿里云 OSS 配置
     */
    @Data
    public static class AliyunOssConfig {

        /**
         * Endpoint（如：oss-cn-hangzhou.aliyuncs.com）
         */
        private String endpoint;

        /**
         * AccessKey ID
         */
        private String accessKeyId;

        /**
         * AccessKey Secret
         */
        private String accessKeySecret;

        /**
         * Bucket 名称
         */
        private String bucketName;

        /**
         * 自定义域名（如：https://cdn.example.com）
         */
        private String domain;
    }

    /**
     * MinIO 配置
     */
    @Data
    public static class MinioConfig {

        /**
         * Endpoint（如：http://localhost:9000）
         */
        private String endpoint;

        /**
         * AccessKey
         */
        private String accessKey;

        /**
         * SecretKey
         */
        private String secretKey;

        /**
         * Bucket 名称
         */
        private String bucketName;
    }

    /**
     * 腾讯云 COS 配置
     */
    @Data
    public static class TencentCosConfig {

        /**
         * 地域（如：ap-guangzhou）
         */
        private String region;

        /**
         * SecretId
         */
        private String secretId;

        /**
         * SecretKey
         */
        private String secretKey;

        /**
         * Bucket 名称
         */
        private String bucketName;

        /**
         * 自定义域名（用于生成签名 URL）
         * <p>
         * 配置后，访问 URL 将使用自定义域名而非 COS 默认域名，
         * 需要在客户端初始化时设置，才能实现自定义域名的 URL 签名
         */
        private String domain;
    }
}
