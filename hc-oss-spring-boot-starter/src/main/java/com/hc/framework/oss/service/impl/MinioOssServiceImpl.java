package com.hc.framework.oss.service.impl;

import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.service.OssService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO OSS服务实现
 */
@Slf4j
public class MinioOssServiceImpl implements OssService {

    private final OssProperties.MinioConfig config;
    private MinioClient minioClient;

    public MinioOssServiceImpl(OssProperties.MinioConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        log.info("MinIO客户端初始化完成");
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (minioClient != null) {
            minioClient.close();
            log.info("MinIO客户端已关闭");
        }
    }

    @Override
    public String upload(String fileName, InputStream inputStream) {
        return upload(fileName, inputStream, "application/octet-stream", -1);
    }

    @Override
    public String upload(String fileName, InputStream inputStream, String contentType) {
        return upload(fileName, inputStream, contentType, -1);
    }

    @Override
    public String upload(String fileName, InputStream inputStream, String contentType, long contentLength) {
        try {
            long objectSize = contentLength > 0 ? contentLength : -1;
            long partSize = contentLength > 0 ? -1 : 5 * 1024 * 1024;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .stream(inputStream, objectSize, partSize)
                            .contentType(contentType)
                            .build()
            );
            return getUrl(fileName);
        } catch (Exception e) {
            log.error("MinIO上传失败: {}", fileName, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("MinIO删除失败: {}", fileName, e);
            return false;
        }
    }

    @Override
    public String getUrl(String fileName) {
        return config.getEndpoint() + "/" + config.getBucketName() + "/" + fileName;
    }

    @Override
    public String getUrl(String fileName, Integer expireTime) {
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .expiry(expireTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO生成临时URL失败: {}", fileName, e);
            throw new RuntimeException("生成临时URL失败", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
