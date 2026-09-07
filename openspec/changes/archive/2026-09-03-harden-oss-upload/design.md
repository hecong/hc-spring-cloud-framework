## Context

见 proposal.md - Why。实现基线（已核对源码）：
- `OssService` 接口已有 `upload(String fileName, InputStream in, String contentType, long contentLength)` 及两个委托重载（`contentLength=-1` 表示未知），**无需新增 size 参数**（改进方案文档基于过时签名，已在 proposal 修正）。
- 三个实现（Aliyun/MinIO/腾讯 COS）四参 `upload` 结构一致：`try { putObject(...); return getUrl(...); } catch (Exception e) { throw new RuntimeException("文件上传失败", e); }`，无校验、无 `finally` 关流。
- 三个 SDK 的 `putObject` 均从流头消费到 EOF：阿里/腾讯经 `metadata` + 流；MinIO 在 `contentLength=-1` 时走 `partSize=5MB` 的分片上传。

## Goals / Non-Goals

**Goals:**
- 校验链"扩展名 → 魔数 → 大小"集中在单一 validator，三实现只做编排，改动可测。
- 在真实 SDK 行为约束内（从头消费流、内部可能包装读流异常）保证 API 表面语义：校验失败恒为 `IllegalArgumentException`。
- 接口签名零变化，业务迁移成本仅来自行为变更。

**Non-Goals:**
- 不做内容安全扫描（杀毒/恶意文件检测）。
- 不改三个 SDK 的选型与上传策略（MinIO 分片参数保持）。
- 不为校验引入重量级依赖；魔数表只覆盖默认白名单中有既定签名的类型。

## Decisions

### D1. 校验链与魔数覆盖策略

`OssUploadValidator`：`validate(fileName, size)` 负责后缀与已知大小；魔数与流式限读在包装流阶段完成（见 D2/D3）。
扩展名 → 魔数映射覆盖有既定签名的二进制/媒体/文档（JPEG `FF D8 FF`、PNG、GIF、PDF `%PDF`、ZIP `PK..`、MP3、MP4 等）；`txt/csv/md` 等无统一签名类型不映射，仅后缀+大小校验。
魔数不足最小签名长度或内容不匹配 → `IllegalArgumentException("文件内容与扩展名不符")`。扩展名不在白名单 → 直接拒绝、不读流。

备选：仅后缀校验——用户已决策需魔数（后缀可伪造），故实现双层。

### D2. 流头读取与回推：PushbackInputStream

前置魔数检测需要读头字节再把流"还"给 SDK。网络流大多不支持 `mark/reset`，因此用 **`PushbackInputStream`**（回推长度 ≥ 最长魔数）包装：读 `maxMagicLen` 字节判签名后 `unread` 回推。SDK 随后从头消费。**本 change 内所有魔数检测都必须在同一包装流上完成一次回推**，避免多层读造成字节错位。

验证项（tasks）：用一个"位置追踪流"断言 SDK 得到的流首字节是原文件首字节（三个实现各自的单测）。

### D3. 大小上限与未知大小流式限制

- `contentLength > 0`：validator 预检即拒，不读流。
- `contentLength == -1`：包装**限制读取流**（累计读超 `maxFileSize+1` 字节即抛大小超限异常并停止读取），SDK 读流过程中触发即中止上传。为满足"不被包装成上传失败"，实现约束：
  - 若 SDK 未包装读流异常（异常沿流对象冒出）→ 上层直接收到大小超限异常；
  - 若某 SDK 将其包装（如包成 OSSException/`RuntimeException`）→ 上传 catch 中检查 cause 链是否含框架标记的"大小超限"异常，命中则改抛 `IllegalArgumentException`，避免语义被吞。
  - 该兼容处理在 tasks 中以"三家 SDK 各自对读流异常的行为验证 + 兜底解包"落地。
- 实现不引入 commons-io（oss 模块当前无此运行时依赖），在 validator 包内提供私有 `BoundedInputStream`。

### D4. 装配与开关

`OssProperties` 增加 `UploadValidationConfig`：`enabled`（默认 true）、`allowedExtensions`（默认内置表）、`maxFileSize`（默认 100MB，`long`，单位字节）。`OssAutoConfiguration` 在有实现 Bean 时装配 validator 并注入三个实现（构造参数）；`enabled=false` 时仍创建实现，但跳过校验（保留关闭行为）。配置对象可选覆盖，空集合/≤0 时回落默认（与改进方案一致）。

备选：`@ConditionalOnProperty` 直接不装配 validator——会让实现类代码分叉（校验代码不存在路径），不利于统一契约，弃用；统一以 `enabled` 分支。

### D5. 异常语义与关流编排

四参 `upload` 结构：
1. 流包装：Pushback 头校验（启用时）→ Bounded 限制（-1 时）→ 校验结果（magic/超限）在此阶段抛 `IllegalArgumentException`。
2. `validator.validate(fileName, contentLength)` 快速失败于 try 之前。
3. SDK 调用 `try { putObject } catch (Exception e) { /* D3 解包兜底 + RuntimeException("文件上传失败") */ } finally { close 原流 }`。

要点：**校验失败路径发生在 SDK try 块之外**（或解包兜底），保证 `IllegalArgumentException` 原样冒出；`finally` 关闭调用方传入的原始流（包装流关闭会级联关闭底层），成功/异常均释放句柄。

## Risks / Trade-offs

- [某 SDK 不从头消费流 / 对流做自身 mark/reset 假设] → Pushback 回推语义 + 位置追踪单测验证三个实现；若单个 SDK 确有特殊行为，task 中针对该实现调整（不得放宽另两家的校验）。
- [SDK 内部包装读流异常导致大小超限语义丢失] → D3 解包兜底，API 表面保证 `IllegalArgumentException`。
- [默认开启导致存量非白名单上传（如临时文件）被拒] → 提供 `enabled=false` 过渡开关 + README 升级说明；扩展白名单可配置覆盖，无需改代码。
- [关流契约破坏"上传后复用流"的存量调用方] → release note 标注 BREAKING，明确"流由框架消费并关闭"。
- [文本类无签名，魔数覆盖不全] → 已按 D1 仅对映射类型校验，文档明示覆盖边界。

## Migration Plan

1. 升级前业务自查：上传文件名后缀均落在默认/自定义白名单内；存在依赖上传后复用流的调用方先改造。
2. 升级时先用 `hc.oss.upload-validation.enabled=false` 平滑验证 → 观察日志 → 逐个业务开启校验并按需配置 `allowed-extensions`/`max-file-size`。
3. 回滚：`enabled=false` 即恢复旧校验行为（关流契约仍生效，属新版本行为）；彻底回滚则还原框架版本。

## Open Questions

- 暂无。三家 SDK 对流异常的包装行为差异在 tasks 阶段以验证用例确认，若出现意外差异仅影响 D3 兜底实现细节，不改变 spec 契约。
