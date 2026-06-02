package com.hc.framework.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
