# hc-common-spring-boot-starter

## 模块简介

`hc-common-spring-boot-starter` 是框架的基础工具库模块，为其他所有模块提供共享的常量、模型和工具类。

本模块是一个**纯工具库**，不包含 Spring Boot 自动配置（无 `AutoConfiguration.imports`），仅作为依赖被其他模块引用。

## 功能特性

- **常量类**：`CacheConstants`、`DateConstants`、`HttpConstants`、`SystemConstants`
- **工具类**：`AssertUtils`、`DateUtils`、`IpUtils`、`JsonUtils`、`PageUtils`、`StringUtils`
- **通用模型**：`Result<T>` 统一响应体、`RepeatSubmitException` 重复提交异常、`DynamicAuthRoute` 动态路由模型

## 快速开始

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-common-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 依赖说明

- Java 17
- Hutool 6.x
- Jackson (JSON 序列化)
- Lombok (provided)
