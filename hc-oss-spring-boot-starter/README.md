# hc-oss-spring-boot-starter

## 模块简介

统一对象存储（OSS）Starter，提供对阿里云 OSS、MinIO、腾讯云 COS 的统一操作接口。

## 功能特性

- **统一接口**：`OssService` 抽象上传/下载/删除等操作
- **多提供商**：支持阿里云 OSS、MinIO、腾讯云 COS，配置切换
- **自动配置**：根据配置自动选择对应的实现类

## 快速开始

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-oss-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

```yaml
hc:
  oss:
    enabled: true
    aliyun:
      enabled: true
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: your-key
      access-key-secret: your-secret
      bucket-name: your-bucket
    # 或使用 MinIO
    minio:
      enabled: true
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket-name: your-bucket
    # 或使用腾讯云 COS
    tencent-cos:
      enabled: true
      region: ap-guangzhou
      secret-id: your-id
      secret-key: your-key
      bucket-name: your-bucket
```

## 依赖说明

- Java 17
- 阿里云 OSS SDK / MinIO SDK / 腾讯云 COS SDK
