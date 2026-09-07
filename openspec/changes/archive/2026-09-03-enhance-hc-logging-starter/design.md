## Context

见 proposal.md - Why 与对应 spec。代码基线（已核对）：
- `TraceIdUtils` 基于 `org.slf4j.MDC`（JDK `ThreadLocal` 语义），`generateTraceId()` = UUID 去横线截 16 位；无任何异步包装能力。
- `TraceIdInterceptor.afterCompletion` 已无条件 `removeTraceId()`（同步清理完备，本 change 保持）。
- `FeignTraceIdInterceptor`/`RestTemplateTraceIdInterceptor` 只 `MDC.get` 后写请求头（P0-4 已对称，不改）。
- `RateLimiterAspect.initFlowRule`：`noneMatch` 检查与 `FlowRuleManager.loadRules` 之间无互斥；`registeredResources` 集合已存在并服务于 `cleanup`。
- `ApiLogAspect`：请求参数全量 `JSONUtil.toJsonStr` + sanitize；响应序列化后硬编码截断 500 字符；无采样。
- 模块已依赖 `org.dromara.hutool:hutool-all`（hutool6），提供 JSON 序列化。

约束：traceId 透传应尽量对业务透明（不要求业务逐处手写 wrap）；不引入 javaagent 运维负担；P0-4/WebClient 不在本 change。

## Goals / Non-Goals

**Goals:**
- traceId 载体升级为"可传递"而不改变 `TraceIdUtils` 现有 API（get/set/remove/initTraceId 兼容）。
- 默认异步场景（业务 `@EnableAsync` + 无自定义执行器）开箱透传；自定义线程池场景提供装饰器接入点。
- API 日志：采样 + 体积上限可配置化，移除硬编码 500。

**Non-Goals:**
- 不支持响应式/WebClient 链路（已列为已知限制，不引入 webflux 依赖）。
- 不做跨线程池的分布式上下文管理（仅解决线程传递，不涉及 RPC/事务等 TTL 完整场景）。
- 不改变 P0-4 出站拦截器（无行为缺口）。

## Decisions

### D1. TraceId 载体：TTL 与 MDC 双写

引入 `com.alibaba:transmittable-thread-local`（**根 pom dependencyManagement 已管理版本 2.14.5，本 change 仅在模块添加依赖、不重复声明版本**）。`TraceIdUtils` 内部新增私有 `TransmittableThreadLocal<String>` 持有当前 traceId，与 MDC **同步双写**：
- `setTraceId`/`removeTraceId`/`initTraceId` 同时操作两处；`getTraceId` 仍读 MDC（保持对外契约与日志格式）。
- TTL 变量随 `TtlRunnable` 在线程间自动传递；MDC 的恢复由装饰器在任务边界显式完成（见 D2）。
- 不采用 `-javaagent` 模式（运维复杂度高、无法保证业务容器统一参数），仅用 TTL 的 API 能力。

备选：仅手工 `wrap`（提交前 capture、任务内 set/remove）不引依赖——用户决策引入 TTL，采用 API 方式在可维护性与侵入性上均衡。

### D2. 异步装配：TaskDecorator + 默认执行器

`LoggingAutoConfiguration` 提供：
1. `TtlTaskDecorator` Bean（`@ConditionalOnMissingBean(TaskDecorator.class)`）：`decorate(Runnable r)` 返回包装——捕获 TTL traceId 后：任务 `run()` 前置 `MDC.put(traceId)`（双写值经 TTL 自动到达），`finally` 中 `MDC.remove` 回滚。
2. 当上下文**不存在任何 `Executor`/`AsyncTaskExecutor`** 时，自动注册默认 `ThreadPoolTaskExecutor`（复用 Spring Boot `TaskExecutionProperties` 风格默认参数）并注入该装饰器，供 `@EnableAsync` 使用；业务自定义执行器时以 `@ConditionalOnMissingBean` 跳过，由业务自行装配装饰器。

风险与落地验证：Spring Boot 的 `TaskExecutionAutoConfiguration` 也会在 `@EnableAsync` 时提供默认 `applicationTaskExecutor`——两套默认执行器的"谁先生效"依赖注册顺序，存在不确定性。设计上：本 change 以"集成启动测试（`@EnableAsync` 上下文 + 无自定义执行器）断言异步日志带 traceId"为准；若与 Boot 默认执行器竞争导致框架执行器未生效，则在任务中回退为"文档化 + `TraceIdUtils.wrap` 便捷方法"路径（tasks 6.x 记录该验证点与 fallback）。

`TraceIdUtils` 同时提供 `wrap(Runnable/Callable)` 便捷方法（内部 `TtlRunnable.get`/`TtlCallable.get` + MDC 边界恢复），供手写 `new Thread`/直接 `executor.submit(TraceIdUtils.wrap(r))` 场景；`Callable` 若抛受检异常按其自身语义传播，`Runnable` 受检异常包装 `RuntimeException`（spec：异常不吞）。

### D3. TraceId 格式

`generateTraceId()` 改为：
- 前 8 位：`String.format("%08x", Instant.now().getEpochSecond())`（秒级 hex，约可覆盖到 2106 年）。
- 后 24 位：`ThreadLocalRandom` 生成 12 字节转 24 hex。
traceId 无需密码学强度（仅日志关联），用 `ThreadLocalRandom` 规避高 QPS 下 `SecureRandom` 竞争（与改进方案 P2-1 一致）。

行为影响：长度 16→32。入站请求头 traceId 沿用原值（可能为旧 16 位），因此**下游解析不得假设固定 32 位**——spec 只约束"生成"格式；README 说明新旧格式混布期间透传不受影响。

### D4. 限流注册去重

`initFlowRule` 重构：`registeredResources.add(resourceName)`（`ConcurrentHashMap.newKeySet` 的 add 具备原子"仅首线程成功"语义）短路；首个成功线程进入 `synchronized (this)` 后**double-check**（再次 `noneMatch`）再追加规则并 `loadRules`，其余线程直接返回。`cleanup`（销毁清理）语义不变。

备选：全量 `synchronized` 包住"检查+加载"（无 CAS 集合）——每次访问都走锁与列表遍历，性能差；CAS 短路 + double-check 是安全与性能的平衡。
风险：若规则被外部（如 Sentinel dashboard）整体清空，已注册集合会短路导致不再补注册——与既有 `cleanup` 语义一致（规则生命周期归本切面管理），文档注明如需支持"外部清空后重注册"可后续扩展监听。

### D5. API 日志采样与体积

**采样**：在 `around` 开始处做一次判定（不再逐段判定）：
- `sampleRate >= 1.0` → 全量。
- 否则以 `traceId.hashCode()`（无符号化）对 1000 取模 < `(int)(sampleRate*1000)` 判定命中；未命中 → `point.proceed()` 直接返回，但 `catch` 分支仍记录异常日志（spec：异常不受采样控制）。
- 采样判定的 traceId 取自 MDC；无 traceId 的调用（非 HTTP 上下文）按全量处理兜底。

**体积上限**：
- `LoggingProperties` 新增 `apiLog` 子配置 `ApiLogConfig`：`sampleRate`（默认 1.0，`double`）、`maxSerializeLength`（默认 4096，`int`）。
- 序列化改造：不使用"先 `toJsonStr` 再 substring"（仍全量物化）。改用**限长写出**：hutool6 `JSONWriter.of(受限Writer)` 分步写出到自定义计数 Writer——累计超过 `maxSerializeLength` 立即抛出内部截断信号并停止，catch 后记录 `<truncated, len=N>`（N 为已写出估算或达到上限值）。若 hutool JSONWriter 对既有脱敏管线（先序列化 JSON 串再正则 sanitize）兼容性不足，回退为：自定义计数 `OutputStreamWriter` + 既有 JSON 序列化工具逐段写出（tasks 中验证一种可行实现即可）。
- 脱敏（sanitize）语义保留：对正常长度内容照常脱敏；截断内容只出标记、不出内容，故无脱敏泄漏面。

### D6. P0-4 验证记录（无代码变更）

出站拦截器对称性结论写入本 change 文档与 README 链路说明："Feign/RestTemplate 仅透传不写上下文；WebClient 暂不支持（已知限制）"。不作为 spec/实现任务。

## Risks / Trade-offs

- [Spring Boot 默认执行器与框架默认执行器竞争] → 集成启动测试验证；失败则文档化接入 `TraceIdUtils.wrap`/装饰器（tasks 6.x）。
- [TTL 版本与 JDK/Spring 版本兼容] → 版本纳入根 pom dependencyManagement；升级说明注明 TTL 版本要求。
- [MDC 双写遗漏某入口导致异步 MDC 为空] → 装饰器是唯一异步边界，统一由 `LoggingAutoConfiguration` 装配；测试覆盖池复用场景断言无残留。
- [限长写出与脱敏管线耦合复杂] → tasks 分两步：先限长实现单测，再接 ApiLogAspect；失败回退路径已记录。
- [traceId 长度变化影响旧系统日志解析] → 非破坏（不依赖固定长度）；README 标注。

## Migration Plan

1. 升级后无需业务改动即获得：traceId 32 位格式、异步透传（默认执行器场景）、限流去重、可配置日志采样（默认全量，行为不变）。
2. 业务可选：将 `sampleRate` 调低以压日志量；自定义线程池接入 `TtlTaskDecorator`。
3. 回滚：还原框架版本即可（新增默认执行器仅在无自定义 Executor 时注册，正常服务不感知）。

## Open Questions

- 是否需要为"外部（Sentinel dashboard）清空规则后自动重注册"提供支持——超出本 change 范围，D4 已注明可按需扩展。
