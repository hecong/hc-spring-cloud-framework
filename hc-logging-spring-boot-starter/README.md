# hc-logging-spring-boot-starter

## 模块简介

提供统一的 API 日志记录、接口限流和全链路 TraceId 传递功能。

## 功能特性

- **API 日志**：自动记录 Controller 请求/响应，敏感参数自动脱敏
- **接口限流**：基于 Sentinel 的 `@RateLimiter` 注解，支持全局限流/IP 限流/用户限流
- **TraceId 传递**：自动在 Feign、RestTemplate、HTTP 请求中传递 TraceId
- **多环境日志**：内置 Logback 配置，dev/test/prod 自动切换
- **SPI 扩展**：`UserIdResolver` 接口，实现后可启用 USER 维度限流

## 快速开始

```xml
<dependency>
    <groupId>com.hc.framework</groupId>
    <artifactId>hc-logging-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

```yaml
hc:
  logging:
    enabled: true
    api-log-enabled: true
    ignore-paths:
      - /actuator/**
    sensitive-param-names:
      - password
      - token
    api-log:                                  # 自 1.1.0 起
      sample-rate: 1.0                        # 采样率 0.0-1.0，默认 1.0 全量；按 traceId 确定性取模
      max-serialize-length: 4096              # 参数/响应 JSON 长度上限（字符），默认 4096
    rate-limit:
      enabled: true
      default-qps: 100
```

### API 日志采样与体积控制（自 1.1.0）

- 采样率 `sample-rate`：按 traceId 哈希取模，**同一链路判定恒定**（跨请求/同链路不抖动）；
  未采样请求仍完整执行业务，仅跳过请求/响应日志；**异常日志不受采样控制**，未采样请求抛异常仍记录，保证故障可观测。
- 体积上限 `max-serialize-length`：请求参数与响应结果序列化超过上限**立即中止**（流式写出，
  不做全量物化后再截断），仅记录 `<truncated, len=N>` 截断标记；原硬编码 500 字符截断已移除。

### 异步线程 TraceId 透传（自 1.1.0）

TraceId 由"仅 MDC"升级为 **MDC + TransmittableThreadLocal 双写**，异步场景开箱即用：

- 框架自动装配：上下文**无任何 `Executor`** 时注册带 TTL 装饰器的默认 `applicationTaskExecutor`
  （与 Boot 同名保证 `@EnableAsync`/按名注入解析确定性），业务无感接入；
- 业务自定义线程池时框架默认执行器自动让位，两种接入方式：
  1. `executor.setTaskDecorator(new TtlTaskDecorator())`；
  2. 手动包装任务：`executor.execute(TraceIdUtils.wrap(task))` / `submit(TraceIdUtils.wrap(callable))`。
- 任务执行期间子线程 MDC 携带同一 traceId，任务结束自动回滚，线程池复用无串扰。

> TTL 采用 API 方式（任务包装）传递，不依赖 `-javaagent` 运维参数；业务无需调整应用启动方式。

### 跨服务透传对称性核对（P0-4 结论）

对 `FeignTraceIdInterceptor` 与 `RestTemplateTraceIdInterceptor` 做了对称性核对：

- **出站方向一致**：Feign 经 `RequestInterceptor`、RestTemplate 经 `RestTemplateCustomizer`
  自动注入拦截器，均从 MDC 读取当前 traceId 并写入 `X-Trace-Id` 请求头；
- **入站方向统一**：`TraceIdInterceptor` 解析 `X-Trace-Id` 请求头并沿用/生成，响应头回传；
  自 1.1.0 起入站 traceId 原样沿用（不再校验长度），新旧版本链路互通；
- **结论**：本版本对两条透传链路**无功能改动**，已由核对结论 + 现有/新增单测锁定；
  `WebClient`（响应式）透传为已知限制，不在本 Starter 支持范围，后续版本另行评估。

## 启用用户维度限流

```java
@Component
public class MyUserIdResolver implements UserIdResolver {
    public String getCurrentUserId() {
        return StpUtil.getLoginIdAsString();
    }
}
```

## TraceId 格式说明（自 1.1.0）

- 新生成 traceId 为 **32 位小写十六进制**（前 8 位秒级时间戳 + 后 24 位随机），日志与请求头
  `X-Trace-Id` 共用；
- **入站请求头携带的 traceId 一律原样沿用**（含老版本 16 位格式），不重新生成，保证跨版本链路不断裂；
- 随机源使用 `ThreadLocalRandom`，规避高 QPS 下 `SecureRandom` 竞争。

## 接口限流

`@RateLimiter` 规则注册为**并发安全幂等**（自 1.1.0）：已注册资源无锁短路，首次并发访问同步块
double-check，消除"检查-加载"窗口竞态重复注册；切面销毁仅清理自身注册规则，不影响其他来源规则。

## 依赖说明

- Java 17
- Sentinel 1.8.8
- Transmittable-Thread-Local 2.14.5
- Spring Cloud OpenFeign（可选）

## 版本历史

- **1.1.0**：异步线程 TraceId 透传（TTL 双写 + 默认执行器/装饰器 + `wrap`）、TraceId 升级 32 位、
  API 日志采样率与限长序列化、`@RateLimiter` 规则注册并发去重
- **1.0.0**：初始版本，提供 API 日志、接口限流与跨服务 TraceId 传递
