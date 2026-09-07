## Purpose

为 OSS 上传链路提供文件类型与大小的可信校验（扩展名白名单 + 文件头魔数 + 大小上限），统一校验开关与异常语义，并明确 InputStream 由框架消费并关闭的生命周期契约。

## ADDED Requirements

### Requirement: 扩展名白名单

系统处理 OSS 上传时，SHALL 校验文件名的扩展名属于允许集合；文件名缺失、无扩展名或扩展名不在白名单内时，SHALL 拒绝上传并抛出校验异常（`IllegalArgumentException`）。系统 SHALL 提供默认白名单，并允许通过配置覆盖。

#### Scenario: 危险扩展名被拒绝

- **WHEN** 上传文件名为 `shell.jsp`、`virus.exe` 或 `noext` 等无扩展名文件名
- **THEN** 上传被拒绝，调用方收到 `IllegalArgumentException`，对象未被写入存储

#### Scenario: 白名单扩展名放行

- **WHEN** 上传 `photo.jpg`、`doc.pdf`、`data.csv` 等白名单内扩展名的文件
- **THEN** 校验通过并继续后续魔数/大小校验

#### Scenario: 自定义白名单生效

- **WHEN** 业务配置 `hc.oss.upload-validation.allowed-extensions` 指定自定义扩展名集合
- **THEN** 校验使用自定义集合而非默认集合

### Requirement: 文件头魔数校验

对具有既定文件签名的文件类型，系统 SHALL 读取文件头部若干字节并与该类型的签名（magic number）比对，内容不匹配时 SHALL 拒绝上传；且校验读取过的字节 SHALL 在交给存储 SDK 前完整回推，使 SDK 从头消费该流。

#### Scenario: 伪造扩展名的文件被识别

- **WHEN** 上传名为 `photo.jpg` 但内容实为可执行脚本（文件头不含 JPEG 签名）的文件
- **THEN** 上传被拒绝并抛 `IllegalArgumentException`（内容与扩展名不符）

#### Scenario: 真实文件校验通过且流从头消费

- **WHEN** 上传内容与扩展名真实匹配的文件（如文件头含 JPEG 签名的 `.jpg`）
- **THEN** 校验通过，且存储 SDK 读取该流时从第一个字节开始（校验读取的头字节已被回推）

#### Scenario: 无统一签名的文本类型仅后缀校验

- **WHEN** 上传 `txt`/`csv`/`md` 等无统一文件签名的文本类型
- **THEN** 不因魔数缺失而拒绝，仅执行扩展名（与大小）校验

### Requirement: 文件大小上限

系统 SHALL 限制单文件大小：已知大小（`contentLength > 0`）超过上限时直接拒绝；未知大小（`contentLength == -1`，流式上传）时 SHALL 在读流过程中计数，读取超过上限即中断并拒绝，不得继续完整上传。

#### Scenario: 已知大小超限拒绝

- **WHEN** 上传文件声明的 `contentLength` 大于配置上限（默认 100MB）
- **THEN** 上传被拒绝并抛 `IllegalArgumentException`，不发起对象写入

#### Scenario: 未知大小流式上传超限中断

- **WHEN** `contentLength == -1` 的流在读取过程中累计字节数超过上限
- **THEN** 读取被中断，上传被拒绝（调用方收到校验失败异常），流被关闭且对象不完整写入存储

#### Scenario: 上限内文件正常上传

- **WHEN** 文件大小在上限以内
- **THEN** 校验通过，继续正常上传流程

### Requirement: 校验开关与异常语义

系统 SHALL 默认开启上传校验，并提供 `hc.oss.upload-validation.enabled` 开关（默认 `true`）；关闭后 SHALL 跳过扩展名/魔数/大小校验。校验失败 SHALL 以 `IllegalArgumentException` 抛给调用方，不得被包装为上传失败类型的运行时异常；存储 SDK/网络等实际上传失败仍保留原有的上传失败异常语义。

#### Scenario: 默认开启

- **WHEN** 未配置任何校验开关即上传非白名单类型
- **THEN** 校验生效并拒绝（默认开启）

#### Scenario: 开关关闭跳过校验

- **WHEN** 配置 `hc.oss.upload-validation.enabled=false` 后上传非白名单类型或超限文件
- **THEN** 校验被跳过，上传继续按原行为执行

#### Scenario: 校验失败异常不被包装

- **WHEN** 上传因类型/内容/大小校验失败
- **THEN** 调用方捕获到的是 `IllegalArgumentException`（消息含拒绝原因），而非"文件上传失败"类运行时异常

#### Scenario: SDK 失败仍保留原语义

- **WHEN** 校验通过但存储服务调用失败（如网络/凭证错误）
- **THEN** 调用方收到上传失败语义的运行时异常（与原行为一致）

### Requirement: 上传流生命周期

系统执行上传时 SHALL 由框架消费并关闭传入的 `InputStream`（成功与异常路径均关闭），调用方在调用返回后不得复用该流；校验前置读取的字节不得导致 SDK 丢失文件开头数据。

#### Scenario: 上传成功后流被关闭

- **WHEN** 上传成功并返回对象地址
- **THEN** 传入的 `InputStream` 已被框架关闭，流资源被释放

#### Scenario: 校验失败/上传异常后流被关闭

- **WHEN** 上传因校验失败或存储异常退出
- **THEN** 传入的 `InputStream` 仍在退出路径被关闭，不发生句柄泄漏
