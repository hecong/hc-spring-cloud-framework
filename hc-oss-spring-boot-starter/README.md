# hc-oss-spring-boot-starter

## 模块简介

统一对象存储（OSS）Starter，提供对阿里云 OSS、MinIO、腾讯云 COS 的统一操作接口。通过单一配置项 `default-type` 选择云服务商，**不允许混用**。

## 设计思路

### 核心设计

```
OssService（统一接口）
    ├── AliyunOssServiceImpl    ← default-type=aliyun
    ├── MinioOssServiceImpl     ← default-type=minio
    └── TencentCosServiceImpl   ← default-type=tencent-cos

选择机制：hc.oss.default-type = aliyun | minio | tencent-cos（单选）
启动校验：所选 provider 的必填配置项缺失 → 启动失败（fast-fail）
```

### 设计原则

- **单选不混用**：`default-type` 是唯一的路由键，同时只会创建一个 `OssService` Bean
- **启动即校验**：构造器阶段检查必填配置，缺失直接抛出 `IllegalStateException` 阻止启动
- **按需引入 SDK**：三个云服务 SDK 均为 `<optional>true</optional>`，消费方按需引入
- **URL 安全编码**：`getUrl()` 对文件名做分段 URL 编码，保留路径分隔符 `/`

## 功能特性

- **统一接口**：`OssService` 提供 `upload` / `delete` / `getUrl` / `exists` 方法
- **三种实现**：阿里云 OSS、MinIO、腾讯云 COS
- **配置驱动**：`default-type` 切换，无需改代码
- **启动校验**：必填配置缺失时启动失败，避免运行时才发现
- **自定义域名**：阿里云 OSS（CNAME）和腾讯云 COS 均支持自定义域名
- **URL 安全**：自动对文件名做 URL 编码，正确处理中文、空格等特殊字符

---

## 快速开始

### 1. 添加依赖

```xml
<!-- 基础依赖（必选） -->
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-oss-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 按需引入 SDK（三选一） -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
</dependency>
<!-- 或 -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
</dependency>
<!-- 或 -->
<dependency>
    <groupId>com.qcloud</groupId>
    <artifactId>cos_api</artifactId>
</dependency>
```

### 2. 配置

```yaml
hc:
  oss:
    enabled: true
    default-type: aliyun      # 必选：aliyun / minio / tencent-cos
    aliyun:
      endpoint: oss-cn-hangzhou.aliyuncs.com
      access-key-id: LTAI5txxxxxxxxxxxx
      access-key-secret: your-secret
      bucket-name: my-bucket
      # domain: https://cdn.example.com   # 可选：自定义域名
```

### 3. 使用

```java
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private OssService ossService;  // 注入唯一的 OssService

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = ossService.upload(
            file.getOriginalFilename(),
            file.getInputStream(),
            file.getContentType(),
            file.getSize()
        );
        return Result.success(url);
    }

    @DeleteMapping("/{fileName}")
    public Result<Void> delete(@PathVariable String fileName) {
        boolean deleted = ossService.delete(fileName);
        return deleted ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/url/{fileName}")
    public Result<String> getUrl(@PathVariable String fileName) {
        // 获取带签名的临时访问 URL（有效期 3600 秒）
        String url = ossService.getUrl(fileName, 3600);
        return Result.success(url);
    }
}
```

---

## 配置说明

### 阿里云 OSS

```yaml
hc:
  oss:
    default-type: aliyun
    aliyun:
      endpoint: oss-cn-hangzhou.aliyuncs.com     # 必填：OSS endpoint
      access-key-id: LTAI5txxxxxxxxxxxx           # 必填：AccessKey ID
      access-key-secret: your-secret              # 必填：AccessKey Secret
      bucket-name: my-bucket                      # 必填：Bucket 名称
      # domain: https://cdn.example.com           # 可选：自定义域名（CNAME）
```

### MinIO

```yaml
hc:
  oss:
    default-type: minio
    minio:
      endpoint: http://localhost:9000             # 必填：MinIO endpoint
      access-key: minioadmin                      # 必填：AccessKey
      secret-key: minioadmin                      # 必填：SecretKey
      bucket-name: my-bucket                      # 必填：Bucket 名称
```

### 腾讯云 COS

```yaml
hc:
  oss:
    default-type: tencent-cos
    tencent-cos:
      region: ap-guangzhou                        # 必填：地域
      secret-id: AKIDxxxxxxxxxxxx                 # 必填：SecretId
      secret-key: your-secret                     # 必填：SecretKey
      bucket-name: my-bucket-1250000000           # 必填：Bucket 名称
      # domain: https://cdn.example.com           # 可选：自定义域名
```

### 完整配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `hc.oss.enabled` | `Boolean` | `true` | 总开关 |
| `hc.oss.default-type` | `String` | `aliyun` | 存储类型：`aliyun` / `minio` / `tencent-cos` |
| `hc.oss.aliyun.endpoint` | `String` | — | 阿里云 OSS endpoint |
| `hc.oss.aliyun.access-key-id` | `String` | — | 阿里云 AccessKey ID |
| `hc.oss.aliyun.access-key-secret` | `String` | — | 阿里云 AccessKey Secret |
| `hc.oss.aliyun.bucket-name` | `String` | — | 阿里云 Bucket 名称 |
| `hc.oss.aliyun.domain` | `String` | — | 阿里云自定义域名（可选） |
| `hc.oss.minio.endpoint` | `String` | — | MinIO endpoint |
| `hc.oss.minio.access-key` | `String` | — | MinIO AccessKey |
| `hc.oss.minio.secret-key` | `String` | — | MinIO SecretKey |
| `hc.oss.minio.bucket-name` | `String` | — | MinIO Bucket 名称 |
| `hc.oss.tencent-cos.region` | `String` | — | 腾讯云 COS 地域 |
| `hc.oss.tencent-cos.secret-id` | `String` | — | 腾讯云 SecretId |
| `hc.oss.tencent-cos.secret-key` | `String` | — | 腾讯云 SecretKey |
| `hc.oss.tencent-cos.bucket-name` | `String` | — | 腾讯云 Bucket 名称 |
| `hc.oss.tencent-cos.domain` | `String` | — | 腾讯云自定义域名（可选） |

---

## OssService 接口

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `upload(fileName, inputStream)` | `String` | 上传文件，返回访问 URL |
| `upload(fileName, inputStream, contentType)` | `String` | 上传文件（指定 Content-Type） |
| `upload(fileName, inputStream, contentType, contentLength)` | `String` | 上传文件（指定 Content-Length） |
| `delete(fileName)` | `boolean` | 删除文件 |
| `getUrl(fileName)` | `String` | 获取文件访问 URL（永久，不含签名） |
| `getUrl(fileName, expireTime)` | `String` | 获取带签名的临时访问 URL（expireTime 单位：秒） |
| `exists(fileName)` | `boolean` | 检查文件是否存在 |

> **注意**：`upload()` 方法的 `InputStream` 由调用方负责关闭，SDK 内部不会关闭传入的流。

---

## 高级用法

### 自定义域名

```yaml
# 阿里云 OSS CNAME
hc:
  oss:
    default-type: aliyun
    aliyun:
      # ...
      domain: https://cdn.example.com

# 腾讯云 COS 自定义域名
hc:
  oss:
    default-type: tencent-cos
    tencent-cos:
      # ...
      domain: https://static.example.com
```

配置自定义域名后，`getUrl()` 和签名 URL 都会使用自定义域名。具体实现：
- **阿里云**：`setSupportCname(true)` + 域名作为 endpoint
- **腾讯云**：`UserSpecifiedEndpointBuilder` + 签名 URL 使用自定义域名

### 启动校验（fast-fail）

启动时自动校验 `default-type` 对应 provider 的必填配置：

```yaml
hc:
  oss:
    default-type: minio
    minio:
      # endpoint 未填 → 启动失败！
      access-key: admin
      secret-key: password
      bucket-name: my-bucket
```

错误信息：

```
java.lang.IllegalStateException: MinIO endpoint 未配置 (hc.oss.minio.endpoint)
```

### 文件上传（指定 Content-Type）

```java
@PostMapping("/upload/image")
public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
    // 保留原始 Content-Type（image/png, image/jpeg 等）
    String url = ossService.upload(
        file.getOriginalFilename(),
        file.getInputStream(),
        file.getContentType()  // 指定 Content-Type
    );
    return Result.success(url);
}
```

### 生成临时访问 URL（私有 Bucket）

```java
@GetMapping("/preview/{fileName}")
public Result<String> preview(@PathVariable String fileName) {
    // 生成 1 小时有效的签名 URL
    String signedUrl = ossService.getUrl(fileName, 3600);
    return Result.success(signedUrl);
}
```

---

## 常见问题

### 1. 如何切换云服务商？

修改 `hc.oss.default-type` 并配置对应 provider 的必填项，重启即生效。

### 2. 能同时使用多个云服务商吗？

**不能**。设计上不允许混用，`defaultType` 是单选。如果需要同时使用多个，请在业务层自行封装。

### 3. 启动时报 IllegalStateException？

说明所选 provider 的必填配置项缺失。请根据异常消息中的配置路径补齐。例如：

```
阿里云OSS endpoint 未配置 (hc.oss.aliyun.endpoint)
```

### 4. 文件名包含中文或空格怎么办？

`getUrl()` 会自动对文件名做分段 URL 编码。路径分隔符 `/` 保留，每个路径段中的特殊字符会被编码为 `%XX` 格式。

### 5. 如何关闭 OSS 模块？

```yaml
hc:
  oss:
    enabled: false
```

配置后不会创建 `OssService` Bean，应用照常启动。

---

## 依赖说明

- Java 17
- 阿里云 OSS SDK 3.17.4（`optional`）
- MinIO SDK 8.5.10（`optional`）
- 腾讯云 COS SDK 5.6.213（`optional`）

---

## 版本历史

- **1.0.0**：初始版本，提供阿里云 OSS、MinIO、腾讯云 COS 统一操作接口
