# hc-rocketmq-spring-boot-starter

## 模块简介

基于 RocketMQ 5.x gRPC 客户端的消息队列 Starter，提供消息发送、消费、事务消息和幂等消费功能。

## 功能特性

- **消息发送**：`RocketMqSender` 统一发送封装
- **消息消费**：`BaseMqConsumer` 消费者基类
- **事务消息**：`BaseTransactionChecker` 事务检查器
- **幂等消费**：基于 Redis 的消息去重（需要 hc-redis-spring-boot-starter）
- **TraceId 传递**：自动在 MQ 消息中携带 TraceId（需要 hc-logging-spring-boot-starter）

## 快速开始

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-rocketmq-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

```yaml
hc:
  rocketmq:
    enabled: true

rocketmq:
  name-server: localhost:9876
  producer:
    group: my-producer-group
  consumer:
    group: my-consumer-group
```

> **注意**：从 1.0-SNAPSHOT 版本开始，框架配置前缀为 `hc.rocketmq.enabled`（而非 `rocketmq.enabled`），与框架其他模块保持一致的 `hc.*` 命名规范。

## 依赖说明

- Java 17
- RocketMQ 5.x gRPC Client
- hc-redis-spring-boot-starter（可选，用于幂等消费）
- hc-logging-spring-boot-starter（可选，用于 TraceId 上下文传递）
