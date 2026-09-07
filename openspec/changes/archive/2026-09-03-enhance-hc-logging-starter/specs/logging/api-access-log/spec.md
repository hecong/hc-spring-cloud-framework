## Purpose

为 API 访问日志提供可控的采样率与序列化体积上限：在高流量下按配置抽样并限制大对象序列化开销与日志体量，同时保留异常日志的完整可观测性。

## ADDED Requirements

### Requirement: 采样率配置

系统 SHALL 支持配置 API 日志采样率（`hc.logging.apiLog.sampleRate`，取值范围 0.0–1.0，默认 1.0 = 全量记录）。采样 SHALL 为确定性：对同一 TraceId 的采样结果一致（同一链路不出现部分请求有日志、部分无日志的抖动）。

#### Scenario: 全量采样默认

- **WHEN** 未配置采样率（默认 1.0）访问接口
- **THEN** 每次请求均输出请求/响应日志

#### Scenario: 按采样率抽样且链路一致

- **WHEN** 配置 `sampleRate=0.5` 后连续请求
- **THEN** 约半数请求输出访问日志；同一 TraceId 跨服务/多次日志的采样判定一致（同链路不出现一半有日志一半无日志）

#### Scenario: 忽略路径不受采样影响

- **WHEN** 访问命中 `ignorePaths` 配置的路径
- **THEN** 该路径按原语义跳过日志（无论采样率如何）

### Requirement: 异常日志不受采样控制

系统 SHALL 保证接口异常日志不受采样率影响——即使该次请求未被采样，发生异常时仍输出异常日志，确保故障可观测。

#### Scenario: 未采样请求仍记录异常

- **WHEN** 一次未被采样的请求处理过程中抛出异常
- **THEN** 仍输出该次请求的异常日志（含 TraceId/方法/URI/耗时）

### Requirement: 序列化体积上限

系统 SHALL 支持配置日志化对象的序列化长度上限（`hc.logging.apiLog.maxSerializeLength`，默认 4096）。当对象序列化长度达到上限时，SHALL 仅记录截断标记（含已达上限的长度信息），不得在日志中输出超限对象的完整内容；序列化过程的实现 SHALL 避免为"事后截断"而对超限对象做全量序列化物化。

#### Scenario: 超长响应只记录截断标记

- **WHEN** 接口响应体序列化长度超过 `maxSerializeLength`
- **THEN** 响应日志中记录 `<truncated, len=...>` 形式的截断标记与长度信息，不包含完整响应内容

#### Scenario: 阈值内正常输出

- **WHEN** 响应/参数序列化长度未超过阈值
- **THEN** 按既有格式完整记录（脱敏规则照常生效）
