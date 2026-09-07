## Why

`hc-logging-spring-boot-starter` 的链路追踪在**同步请求**生命周期已正确清理（`TraceIdInterceptor.afterCompletion` 无条件 `removeTraceId`），但存在四类问题：①**异步线程无 TraceId 透传**——`@Async`/线程池内 MDC 为空，日志丢失链路关联；②`generateTraceId()` 用 UUID 截断 16 位（64bit），碰撞面大且不可读；③`RateLimiterAspect.initFlowRule` 的"检查规则不存在"与"加载规则"非原子，高并发首访会重复注册；④`ApiLogAspect` 无采样、大响应序列化与日志体量不受控（截断阈值 500 字符硬编码且发生在全量序列化之后）。

## What Changes

- **异步 TraceId 透传**：引入 TTL（transmittable-thread-local）依赖；traceId 载体由"仅 MDC"改为"MDC + 可传递上下文双写"；提供 `TraceIdUtils.wrap(Runnable/Callable)` 与自动配置的 `TaskDecorator`/默认异步执行器 Bean（业务无自定义线程池时开箱透传，自定义池引用装饰器即可）。
  - 被包装任务执行前从提交线程复制 traceId，执行后回滚，不污染线程池复用；任务异常不被吞掉（`Runnable` 内受检异常包装为 `RuntimeException` 重抛以保持可观测）。
  - 同步请求生命周期行为不变（`afterCompletion` 清理保留）。
- **TraceId 格式**：`generateTraceId()` 改为 32 位十六进制（8 位秒级时间戳 + 24 位随机，随机源不依赖密码学强度），消除截断碰撞且日志可读；请求头携带的 traceId 继续原样透传，不重新生成。
- **限流规则注册竞态**：`RateLimiterAspect` 用本地去重集合（CAS 语义）短路已注册资源，注册与加载进同步块，杜绝并发重复注册；`cleanup` 清理逻辑保留。
- **API 日志采样与体积控制**：`LoggingProperties` 新增 `apiLog.sampleRate`（默认 1.0）与 `apiLog.maxSerializeLength`（默认 4096）；采样为**确定性**（同一 traceId 是否采样恒定，保证一条链路口径一致）；请求/响应/异常日志受采样开关控制；序列化超长时只记录 `<truncated, len=N>`，避免超限对象被全量序列化与写日志（限制写出，非事后截断）。
- **P0-4 出站透传**：经核对 `FeignTraceIdInterceptor`/`RestTemplateTraceIdInterceptor` 只读 MDC、只写 header，与接收端对称、无 MDC 污染，**无功能改动**；本次以文档形式记录验证结论；WebClient/响应式栈透传列为已知限制，不引入 webflux。

## Capabilities

### New Capabilities

- `logging/trace-context`：TraceId 的生成格式、MDC 生命周期与异步线程上下文透传（含清理回滚语义）。
- `logging/rate-limiting`：注解式限流的规则注册并发安全与唯一性。
- `logging/api-access-log`：API 访问日志的确定性采样、序列化体积上限与截断标记语义。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **依赖**：`hc-logging-spring-boot-starter` 新增 `com.alibaba:transmittable-thread-local`（非 optional，随 starter 传递）；版本纳入根 pom 依赖管理。
- **代码**：`util/TraceIdUtils`、`interceptor/TraceIdInterceptor`、`config/LoggingAutoConfiguration`（async executor/TaskDecorator 装配）、`config/LoggingProperties`（`apiLog` 配置组）、`aspect/ApiLogAspect`（采样与限制写出）、`aspect/RateLimiterAspect`（注册去重）。
- **行为变更提示**：traceId 长度由 16 位变 32 位——对已落库日志/按长度校验的下游系统需评估，属**非破坏性**（header 透传与解析不依赖固定长度）；`apiLog.sampleRate<1.0` 时接口日志会抽样缺失（按业务预期配置）。
- **发布影响**：新增默认异步执行器 Bean 仅在有 `@EnableAsync` 且无自定义执行器的上下文生效，正常服务无感知；README 需给出 TTL 版本说明与自定义线程池接入方式。
