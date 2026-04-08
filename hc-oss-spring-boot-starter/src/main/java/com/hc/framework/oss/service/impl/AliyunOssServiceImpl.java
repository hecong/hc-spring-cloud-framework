package com.hc.framework.oss.service.impl;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.service.OssService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云OSS服务实现
 */
@Slf4j
public class AliyunOssServiceImpl implements OssService {

    private final OssProperties.AliyunOssConfig config;
    private OSS ossClient;

    public AliyunOssServiceImpl(OssProperties.AliyunOssConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 开启CNAME选项以支持自定义域名访问
        clientBuilderConfiguration.setSupportCname(true);

        String endpoint = config.getEndpoint();
        // 如果配置了自定义域名，使用自定义域名作为Endpoint
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            endpoint = config.getDomain();
        }

        this.ossClient = new OSSClientBuilder().build(
                endpoint,
                config.getAccessKeyId(),
                config.getAccessKeySecret(),
                clientBuilderConfiguration
        );
        log.info("阿里云OSS客户端初始化完成, endpoint: {}", endpoint);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云OSS客户端已关闭");
        }
    }

    @Override
    public String upload(String fileName, InputStream inputStream) {
        return upload(fileName, inputStream, "application/octet-stream");
    }

    @Override
    public String upload(String fileName, InputStream inputStream, String contentType) {
        try {
            ossClient.putObject(config.getBucketName(), fileName, inputStream);
            return getUrl(fileName);
        } catch (Exception e) {
            log.error("阿里云OSS上传失败: {}", fileName, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        try {
            ossClient.deleteObject(config.getBucketName(), fileName);
            return true;
        } catch (Exception e) {
            log.error("阿里云OSS删除失败: {}", fileName, e);
            return false;
        }
    }

    @Override
    public String getUrl(String fileName) {
        if (config.getDomain() != null && !config.getDomain().isEmpty()) {
            return config.getDomain() + "/" + fileName;
        }
        return "https://" + config.getBucketName() + "." + config.getEndpoint() + "/" + fileName;
    }

    @Override
    public String getUrl(String fileName, Integer expireTime) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000);
            URL url = ossClient.generatePresignedUrl(config.getBucketName(), fileName, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("阿里云OSS生成临时URL失败: {}", fileName, e);
            throw new RuntimeException("生成临时URL失败", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        try {
            return ossClient.doesObjectExist(config.getBucketName(), fileName);
        } catch (Exception e) {
            log.error("阿里云OSS检查文件存在性失败: {}", fileName, e);
            return false;
        }
    }
}
