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
    <groupId>com.hnhegui.framework</groupId>
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
    rate-limit:
      enabled: true
      default-qps: 100
```

## 启用用户维度限流

```java
@Component
public class MyUserIdResolver implements UserIdResolver {
    public String getCurrentUserId() {
        return StpUtil.getLoginIdAsString();
    }
}
```

## 依赖说明

- Java 17
- Sentinel 1.8.8
- Spring Cloud OpenFeign（可选）
