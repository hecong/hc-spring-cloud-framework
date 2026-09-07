## 1. TTL 依赖与 traceId 载体（trace-context）

- [x] 1.1 在 `hc-logging` pom 添加 `com.alibaba:transmittable-thread-local` 非 optional 依赖（版本由根 pom dependencyManagement 已管理的 2.14.5 提供，先核对与 JDK21/Spring Boot 4 兼容性，如不兼容再更新根 pom 版本）；验证：`mvn -pl hc-logging-spring-boot-starter dependency:tree` 含该依赖且全量编译通过
- [x] 1.2 `TraceIdUtils` 增加私有 `TransmittableThreadLocal` 载体并与 MDC 双写：`setTraceId`/`removeTraceId`/`initTraceId` 同步维护两处，`getTraceId` 契约不变；验证：既有依赖 `TraceIdUtils` 的调用方单测全绿（API 不变）
- [x] 1.3 实现 `TraceIdUtils.wrap(Runnable)`/`wrap(Callable)`：经 TTL 捕获传递 + 任务边界 MDC 恢复/回滚；`Runnable` 内受检异常包装为 `RuntimeException` 重抛；验证：单测断言（a）包装任务执行时 MDC 含提交线程 traceId；（b）任务结束后 MDC 已回滚；（c）异常不被吞

## 2. 自动装配（trace-context）

- [x] 2.1 `LoggingAutoConfiguration` 注册 `TtlTaskDecorator` Bean（`@ConditionalOnMissingBean(TaskDecorator.class)`），内部基于 TTL + MDC 边界恢复；验证：上下文加载测试可获取该 Bean
- [x] 2.2 当上下文无任何 `Executor`/`AsyncTaskExecutor` 时自动注册默认 `ThreadPoolTaskExecutor`（注入装饰器）；验证：集成测试（`@EnableAsync`、无自定义执行器）中异步任务日志包含提交线程 traceId
- [x] 2.3 若与 Spring Boot 默认 `applicationTaskExecutor` 竞争导致框架执行器未生效，落地文档化回退路径（README 指引自定义池注入装饰器 / 使用 `wrap`）；验证：README 异步接入章节存在且步骤可执行（`@AutoConfigureBefore` 已消除竞争，框架默认执行器实测生效）
- [x] 2.4 线程池复用场景验证：异步任务完成后线程复用于无 traceId 上下文的任务，断言无残留 traceId；验证：集成测试通过

## 3. TraceId 格式（trace-context）

- [x] 3.1 `generateTraceId()` 改为 32 位 hex（8 位秒级时间戳 + 24 位 `ThreadLocalRandom` 随机）；验证：单测断言长度为 32、全小写 hex、前 8 位等于当前 epochSecond hex、同秒并发无重复
- [x] 3.2 保留"请求头 traceId 原样沿用"逻辑并补单测（旧 16 位请求头 traceId 不被改写）；验证：`initTraceId` 单测通过

## 4. 限流注册去重（rate-limiting）

- [x] 4.1 重构 `RateLimiterAspect.initFlowRule`：`registeredResources.add` CAS 短路 + `synchronized` 内 double-check（noneMatch）后追加规则并 `loadRules`；验证：并发 50 线程首次访问同一限流资源后规则管理器该资源规则计数为 1
- [x] 4.2 验证重复访问短路与 `cleanup` 语义：重复访问不再触发规则加载；切面销毁仅清理本切面注册资源，其他来源规则保留；验证：对应单测通过

## 5. API 日志采样与体积（api-access-log）

- [x] 5.1 `LoggingProperties` 新增 `apiLog` 配置组（`sampleRate` 默认 1.0、`maxSerializeLength` 默认 4096）；验证：配置绑定单测 + metadata 生成（hc.logging.api-log.* 已产出）
- [x] 5.2 实现限长序列化工具（优先 hutool6 `JSONWriter.of(受限 Writer)`，累计超限即停止并抛内部截断信号；不可行则用自定义计数 Writer + 既有序列化逐段写出）；验证：单测证明超过 `maxSerializeLength` 时序列化被提前终止而非全量物化（hutool6 M12 无 JSONWriter，采用计数 Writer + JSONUtil.toJsonStr(Writer)/InternalJSONUtil.quote 流式路径）
- [x] 5.3 `ApiLogAspect` 接入采样判定（traceId 确定性取模，未命中直接 proceed，异常分支仍记录）与限长写出（超限记 `<truncated, len=...>`，移除硬编码 500 截断）；验证：单测覆盖（a）默认 1.0 全量；（b）0.5 下同一 traceId 判定一致；（c）未采样请求异常仍出日志；（d）超长响应仅出截断标记；（e）阈值内内容+脱敏照常
- [x] 5.4 移除旧硬编码截断路径并确认 `ignorePaths`/sanitize 语义未回归；验证：ApiLogAspect 相关既有用例全绿

## 6. 文档与验证（含 P0-4 记录）

- [x] 6.1 README 记录 P0-4 结论（Feign/RestTemplate 透传对称已核对，无功能改动；WebClient 为已知限制）；验证：文档章节存在
- [x] 6.2 README 记录 traceId 格式变更（16→32 位，不依赖下游固定长度）、TTL 版本要求、采样/截断配置示例与自定义线程池接入方式；验证：README 升级说明完整
- [x] 6.3 运行 `mvn -pl hc-logging-spring-boot-starter test` 与全量 `mvn verify`；验证：本 change 与回归用例全绿
