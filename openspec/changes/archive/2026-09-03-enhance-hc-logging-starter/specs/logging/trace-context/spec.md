## Purpose

定义 TraceId 的生成格式、同步请求生命周期清理与异步线程上下文透传语义，保证日志链路在同步与线程池场景下完整关联且互不污染。

## ADDED Requirements

### Requirement: TraceId 生成格式

系统新生成的 TraceId SHALL 为 32 位十六进制字符串（不含连字符），其中前 8 位为秒级 Unix 时间戳的十六进制表示（不足补零），后 24 位为随机十六进制；同一秒内不同调用生成的 TraceId SHALL 彼此不同。

#### Scenario: 生成符合格式

- **WHEN** 调用生成 TraceId
- **THEN** 结果为 32 位小写十六进制字符串，无连字符，前 8 位等于当前秒级时间戳的十六进制

#### Scenario: 无截断碰撞

- **WHEN** 在同一秒内并发生成大量 TraceId
- **THEN** 返回的 TraceId 因 24 位随机后缀而不重复

### Requirement: 入站 TraceId 原样沿用

系统收到携带请求头 TraceId 的入站请求时，SHALL 沿用该值写入链路上下文，不得重新生成或改写其内容。

#### Scenario: 携带请求头的请求沿用原值

- **WHEN** 请求头携带合法 TraceId 进入服务
- **THEN** 请求处理期间日志中的 TraceId 与请求头一致，且响应头回写同一值

### Requirement: 同步请求生命周期清理

系统 SHALL 在每个 HTTP 请求处理开始建立 TraceId 上下文、结束后无条件清除该线程的 TraceId，防止线程复用导致上下文串扰。

#### Scenario: 请求结束后上下文被清除

- **WHEN** 请求处理完成（含异常路径）进入线程池复用
- **THEN** 该线程的 TraceId 上下文已被清除，下一请求未显式生成前日志不出现上一请求的 TraceId

### Requirement: 异步任务上下文透传

系统 SHALL 支持线程池/异步任务执行时继承提交线程的 TraceId：通过框架提供的包装（`wrap`）或装饰器提交的任务，SHALL 在任务执行开始时具备与提交线程一致的 TraceId，任务执行结束后 SHALL 回滚该线程上下文（不影响线程池复用）。

#### Scenario: 异步任务日志关联

- **WHEN** 业务在带 TraceId 的请求内通过 `@Async`/框架线程池提交任务
- **THEN** 任务执行期间日志包含与提交线程一致的 TraceId

#### Scenario: 任务结束上下文回滚

- **WHEN** 异步任务执行完成后该线程被线程池复用执行另一个无 TraceId 上下文的任务
- **THEN** 新任务日志中不出现前一个任务的残留 TraceId

### Requirement: 异步任务异常不吞

系统包装异步任务时 SHALL 不吞掉任务异常：任务内抛出的运行时异常保持传播；`Runnable` 包装中的受检异常 SHALL 以 `RuntimeException` 重新抛出，保证异常可观测且不会静默丢失。

#### Scenario: 包装任务异常保持可观测

- **WHEN** 被包装的异步任务抛出异常
- **THEN** 异常被线程池/调用方捕获到（受检异常已按 `RuntimeException` 语义重抛），且任务执行后上下文仍被回滚
