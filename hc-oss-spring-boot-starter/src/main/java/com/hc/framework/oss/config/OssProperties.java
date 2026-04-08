package com.hc.framework.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS配置属性
 */
@Data
@ConfigurationProperties(prefix = "hc.oss")
public class OssProperties {

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 默认存储类型
     */
    private String defaultType = "aliyun";

    /**
     * 阿里云OSS配置
     */
    private AliyunOssConfig aliyun = new AliyunOssConfig();

    /**
     * MinIO配置
     */
    private MinioConfig minio = new MinioConfig();

    /**
     * 腾讯云COS配置
     */
    private TencentCosConfig tencentCos = new TencentCosConfig();

    /**
     * 阿里云OSS配置
     */
    @Data
    public static class AliyunOssConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;

        /**
         * Endpoint
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
         * Bucket名称
         */
        private String bucketName;

        /**
         * 自定义域名
         */
        private String domain;
    }

    /**
     * MinIO配置
     */
    @Data
    public static class MinioConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;

        /**
         * Endpoint
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
         * Bucket名称
         */
        private String bucketName;
    }

    /**
     * 腾讯云COS配置
     */
    @Data
    public static class TencentCosConfig {
        /**
         * 是否启用
         */
        private Boolean enabled = false;

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
         * Bucket名称
         */
        private String bucketName;

        /**
         * 自定义域名（用于生成签名URL）
         * <p>
         * 配置后，访问URL将使用自定义域名而非COS默认域名，
         * 需要在客户端初始化时设置，才能实现自定义域名的URL签名
         */
        private String domain;
    }
}
