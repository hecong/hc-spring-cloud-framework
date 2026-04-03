package com.hc.framework.oss.service.impl;

import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.service.OssService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.endpoint.UserSpecifiedEndpointBuilder;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 腾讯云COS服务实现类
 * <p>
 * 支持自定义域名配置，在客户端初始化时注入自定义域名，
 * 实现自定义域名的URL签名访问
 *
 * @author hc
 */
@Slf4j
public class TencentCosServiceImpl implements OssService {

    private final COSClient cosClient;
    private final OssProperties.TencentCosConfig config;

    /**
     * COS API服务端点（固定值）
     */
    private static final String SERVICE_API_ENDPOINT = "cos.tencentcloudapi.com";

    public TencentCosServiceImpl(OssProperties.TencentCosConfig config) {
        this.config = config;
        this.cosClient = createCosClient(config);
        log.info("腾讯云COS客户端初始化完成，region={}", config.getRegion());
    }

    /**
     * 创建COS客户端
     * <p>
     * 如果配置了自定义域名，会在客户端初始化时设置EndpointBuilder，
     * 这样生成的签名URL会使用自定义域名
     */
    private COSClient createCosClient(OssProperties.TencentCosConfig config) {
        // 1. 创建凭证
        COSCredentials credentials = new BasicCOSCredentials(config.getSecretId(), config.getSecretKey());

        // 2. 创建客户端配置
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));

        // 3. 配置自定义域名（关键步骤）
        String domain = config.getDomain();
        if (domain != null && !domain.isBlank()) {
            String userEndpoint = extractHost(domain);
            if (userEndpoint != null) {
                // 设置自定义端点构建器，使签名URL使用自定义域名
                clientConfig.setEndpointBuilder(
                        new UserSpecifiedEndpointBuilder(userEndpoint, SERVICE_API_ENDPOINT)
                );
                log.info("腾讯云COS已配置自定义域名: {}", userEndpoint);
            }
        }

        // 4. 创建客户端
        return new COSClient(credentials, clientConfig);
    }

    /**
     * 从域名中提取主机部分
     *
     * @param domain 完整域名（如：https://cdn.example.com）
     * @return 主机部分（如：cdn.example.com）
     */
    private String extractHost(String domain) {
        if (domain == null || domain.isBlank()) {
            return null;
        }
        try {
            // 移除协议前缀
            String host = domain.trim();
            if (host.startsWith("http://")) {
                host = host.substring(7);
            } else if (host.startsWith("https://")) {
                host = host.substring(8);
            }
            // 移除路径部分
            int slashIndex = host.indexOf('/');
            if (slashIndex > 0) {
                host = host.substring(0, slashIndex);
            }
            // 移除端口号
            int portIndex = host.indexOf(':');
            if (portIndex > 0) {
                host = host.substring(0, portIndex);
            }
            return host.isBlank() ? null : host;
        } catch (Exception e) {
            log.warn("解析自定义域名失败: {}", domain, e);
            return null;
        }
    }

    @Override
    public String upload(String fileName, InputStream inputStream) {
        return upload(fileName, inputStream, "application/octet-stream");
    }

    @Override
    public String upload(String fileName, InputStream inputStream, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());

            PutObjectRequest request = new PutObjectRequest(
                    config.getBucketName(),
                    fileName,
                    inputStream,
                    metadata
            );

            cosClient.putObject(request);
            log.info("腾讯云COS文件上传成功: {}", fileName);

            return getUrl(fileName);
        } catch (Exception e) {
            log.error("腾讯云COS文件上传失败: {}", fileName, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public boolean delete(String fileName) {
        try {
            cosClient.deleteObject(config.getBucketName(), fileName);
            log.info("腾讯云COS文件删除成功: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("腾讯云COS文件删除失败: {}", fileName, e);
            return false;
        }
    }

    @Override
    public String getUrl(String fileName) {
        // 如果配置了自定义域名，返回自定义域名的URL
        if (config.getDomain() != null && !config.getDomain().isBlank()) {
            String domain = config.getDomain().trim();
            // 确保域名以 / 结尾
            if (!domain.endsWith("/")) {
                domain = domain + "/";
            }
            // 确保文件名不以 / 开头
            String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
            return domain + key;
        }
        // 否则返回COS默认域名URL
        return String.format("https://%s.cos.%s.myqcloud.com/%s",
                config.getBucketName(), config.getRegion(), fileName);
    }

    @Override
    public String getUrl(String fileName, Integer expireTime) {
        try {
            // 生成带签名的URL
            Date expirationDate = new Date(System.currentTimeMillis() + expireTime * 1000);
            URL url = cosClient.generatePresignedUrl(
                    config.getBucketName(),
                    fileName,
                    expirationDate,
                    HttpMethodName.GET
            );
            return url.toString();
        } catch (Exception e) {
            log.error("腾讯云COS生成签名URL失败: {}", fileName, e);
            throw new RuntimeException("生成签名URL失败", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        try {
            return cosClient.doesObjectExist(config.getBucketName(), fileName);
        } catch (Exception e) {
            log.error("腾讯云COS检查文件存在性失败: {}", fileName, e);
            return false;
        }
    }
}
