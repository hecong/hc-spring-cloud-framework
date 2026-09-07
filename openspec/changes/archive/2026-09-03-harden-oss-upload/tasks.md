## 1. 校验组件（upload-security）

- [x] 1.1 新增 `OssUploadValidator`（`support` 包）：扩展名白名单（默认集合含 jpg/png/pdf/docx/zip/mp4 等，不含 jsp/exe/无后缀）、`contentLength > 0` 的大小预检；`allowedExtensions` 为空或 `maxFileSize<=0` 时回落默认；验证：单测覆盖 `.jsp`/`.exe`/无后缀拒绝、白名单放行、自定义集合覆盖默认、声明大小超限拒绝
- [x] 1.2 魔数表与 `PushbackInputStream` 头校验：映射有既定签名类型（JPEG/PNG/GIF/BMP/WebP/PDF/ZIP/DOC(X)/XLS(X)/PPT(X)/RAR/7z/MP3/MP4），读头字节比对后 `unread` 回推，文本类（txt/csv/md）跳过魔数；验证：伪造后缀（内容非 JPEG 的 `.jpg`）抛 `IllegalArgumentException("内容与扩展名不符")`，真实 `.jpg` 校验通过且回推后流从头可读
- [x] 1.3 未知大小限制读取流：validator 内提供 `BoundedInputStream`，累计读取超过上限即抛框架标记的 `UploadSizeLimitExceededException` 并停止读取；验证：`-1` 流式上传超限时读取被中断、流被关闭

## 2. 配置与装配

- [x] 2.1 `OssProperties` 增加 `uploadValidation`（`enabled` 默认 true、`allowedExtensions`、`maxFileSize` 默认 100MB）；验证：`@ConfigurationProperties` 绑定单测通过、metadata 已生成（含 allowed-extensions/enabled/max-file-size）
- [x] 2.2 `OssAutoConfiguration` 装配 validator 并注入三个实现，`enabled=false` 时跳过校验但服务仍创建；验证：ApplicationContextRunner 两种开关分支均创建服务，自定义白名单/大小上限正确透传

## 3. 三个实现类改造

- [x] 3.1 改造 `AliyunOssServiceImpl` 四参 `upload`：校验失败在 SDK 调用前抛 `IllegalArgumentException`，外层 finally 关闭传入流；验证：位置追踪流单测证明 SDK 从头消费（头字节未丢失）、校验失败与成功路径流均被关闭
- [x] 3.2 同步改造 `MinioOssServiceImpl`（`-1` 分片场景走限制读流）；验证同上 + MinIO 流式超限用例
- [x] 3.3 同步改造 `TencentCosServiceImpl`；验证同上
- [x] 3.4 三家 SDK 读流抛异常的包装处理：catch 中扫描 cause 链识别框架标记异常并改抛 `IllegalArgumentException`，保证 API 表面校验失败异常类型不变；验证：每个实现均覆盖"伪造后缀被拒且异常为 `IllegalArgumentException`、SDK 零交互"用例

## 4. 文档与发布说明

- [x] 4.1 README 增加 oss 上传校验说明：默认开启、配置项示例（含自定义扩展名/大小上限）、`enabled=false` 过渡路径、BREAKING 提示（默认校验 + 框架关流契约，调用方不得复用传入流）；验证：README 顶部发布顺序与使用说明可被业务引用
- [x] 4.2 补充无统一签名文本类型的覆盖边界说明（txt/csv/md 仅后缀+大小校验，自定义无签名扩展名同规则）；验证：文档"上传校验行为说明"小节存在

## 5. 全量验证

- [x] 5.1 运行 `mvn -pl hc-oss-spring-boot-starter test` 全绿（33 用例）；验证：本 change 相关单测通过
- [x] 5.2 运行全量 `mvn verify` 无回归；验证：构建通过
