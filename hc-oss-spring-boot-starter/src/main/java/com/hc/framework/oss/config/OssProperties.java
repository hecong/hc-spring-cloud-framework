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
}
