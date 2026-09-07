## Why

`hc-oss-spring-boot-starter` 的三个实现（阿里云/MinIO/腾讯 COS）上传链路没有任何防护：文件类型只信扩展名（可伪造）、不校验大小、上传后不关闭 `InputStream`（句柄泄漏）；且所有异常被 catch 后统一包装成 `RuntimeException("文件上传失败")`，业务无法区分"校验失败"与"上传失败"，也无法按业务错误码处理。

## What Changes

- 新增统一上传校验组件 `OssUploadValidator`（三个实现共用），校验链：**扩展名白名单 → 文件头魔数（magic number）→ 大小上限**。
  - 扩展名白名单默认：图片/文档/文本/压缩/音视频常见后缀（jpg/png/pdf/docx/zip/mp4 等），`.jsp`/`.exe`/无后缀拒绝。
  - 魔数校验：对具有既定文件签名的类型读取文件头字节与真实内容比对（防扩展名伪造），比对前通过回推缓冲把流"还回"给 SDK 从头消费；无统一签名的文本类（txt/csv/md）仅做后缀校验。
  - 大小上限默认 100MB，可配置。
- 未知大小的流式上传（`contentLength == -1`）：用**限制读取流**包装（如 BoundedInputStream），读取字节数超过上限即中断并拒绝，不做全量上传。
- **BREAKING** 校验默认开启；新增 `hc.oss.upload-validation.enabled=false` 开关（如临时文件/非白名单业务场景可关闭校验，但关闭后仍保留流关闭行为）。
- **BREAKING** 校验失败（类型/大小/内容不符）抛 `IllegalArgumentException`，**不被包装**成 `RuntimeException`；仅 SDK/网络上传失败保留现有 `RuntimeException` 语义。
- **BREAKING** `InputStream` 生命周期契约变更：由框架在 upload 方法内 `finally` 关闭（现状从不关闭），调用方传入后不得复用该流。接口签名不变（现有 4 参含 `contentLength`，无需新增 size 参数）。

## Capabilities

### New Capabilities

- `oss/upload-security`：OSS 上传的扩展名/魔数/大小校验、校验开关与异常语义、InputStream 关闭契约。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：`hc-oss-spring-boot-starter`
  - 新增 `support/OssUploadValidator`（校验逻辑 + 魔数表 + 限制读取流封装）
  - `config/OssProperties`：新增 `uploadValidation`（enabled/allowedExtensions/maxFileSize）配置组
  - `config/OssAutoConfiguration`：按开关装配校验器
  - `service/impl/AliyunOssServiceImpl`、`MinioOssServiceImpl`、`TencentCosServiceImpl`：四参 `upload` 插入校验、恢复异常语义、`finally` 关闭流
- **依赖**：不新增运行时依赖（魔数表与限制读自实现或复用模块既有工具，无签名文本类仅后缀校验）。
- **配置**：新增 `hc.oss.upload-validation.enabled`（默认 true）、`hc.oss.upload-validation.allowed-extensions`、`hc.oss.upload-validation.max-file-size`。
- **发布影响**：默认开启校验与关流契约均属破坏性——升级说明需标注：存量调用方若上传非白名单类型需先配置扩展；依赖"上传后复用流"的代码需改造；README 提供临时 `enabled=false` 过渡路径。
