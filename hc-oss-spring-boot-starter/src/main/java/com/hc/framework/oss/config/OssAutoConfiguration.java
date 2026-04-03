package com.hc.framework.oss.config;

import com.hc.framework.oss.service.OssService;
import com.hc.framework.oss.service.impl.AliyunOssServiceImpl;
import com.hc.framework.oss.service.impl.MinioOssServiceImpl;
import com.hc.framework.oss.service.impl.TencentCosServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OSS Starter 自动配置类
 * 提供阿里云OSS、MinIO、腾讯云COS文件存储支持
 */
@AutoConfiguration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "hc.oss", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OssAutoConfiguration {

    /**
     * 阿里云OSS服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.oss.aliyun", name = "enabled", havingValue = "true")
    public OssService aliyunOssService(OssProperties ossProperties) {
        return new AliyunOssServiceImpl(ossProperties.getAliyun());
    }

    /**
     * MinIO服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.oss.minio", name = "enabled", havingValue = "true")
    public OssService minioOssService(OssProperties ossProperties) {
        return new MinioOssServiceImpl(ossProperties.getMinio());
    }

    /**
     * 腾讯云COS服务
     * <p>
     * 支持自定义域名配置，在客户端初始化时注入自定义域名，
     * 实现自定义域名的URL签名访问
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hc.oss.tencent-cos", name = "enabled", havingValue = "true")
    public OssService tencentCosService(OssProperties ossProperties) {
        return new TencentCosServiceImpl(ossProperties.getTencentCos());
    }
}
