## Purpose

约束 Redis 值 JSON 多态反序列化允许的类型范围，防止攻击者借 `@class` 元数据触发任意类型实例化（RCE），同时保留白名单内业务类型的序列化往返能力。

## ADDED Requirements

### Requirement: 多态类型白名单

系统在反序列化 Redis 存储的 JSON 值时，SHALL 仅实例化白名单内包前缀的类型；白名单外的类型 SHALL 被拒绝并抛出反序列化异常，且不得实例化目标类型或执行其构造链。

#### Scenario: 白名单外的危险类型被拒绝

- **WHEN** Redis 中的 JSON 值带有指向白名单外类型的 `@class`（例如 `javax.naming.Reference` 之类可触发 JNDI/gadget 链的类型）
- **THEN** 反序列化抛出类型校验异常（如 `InvalidTypeIdException`），且目标类型未被实例化

#### Scenario: 白名单内的类型正常往返

- **WHEN** 序列化/反序列化白名单内的业务 DTO（含嵌套集合、`LocalDateTime` 等字段）
- **THEN** 序列化再反序列化后内容与原值一致，无类型信息丢失

### Requirement: 默认白名单内容

系统 SHALL 在无任何业务配置时，默认放行以下包前缀：`com.hc.framework.`、`com.hnhegui.`、`java.util.`、`java.lang.`、`java.time.`。

#### Scenario: 未配置时的开箱行为

- **WHEN** 业务未配置扩展白名单，反序列化 `com.hc.framework.*` 或 `com.hnhegui.*` 前缀的业务对象
- **THEN** 反序列化成功

### Requirement: 业务白名单追加

系统 SHALL 提供配置项 `hc.redis.allowed-packages`；业务配置的值 SHALL 与默认白名单取并集（追加式），而非替换默认值。

#### Scenario: 追加业务包

- **WHEN** 业务配置 `hc.redis.allowed-packages` 含自身实体包（如 `com.your.business.`）
- **THEN** 该包类型与默认白名单类型均可正常反序列化；漏配 JDK 基础包不影响默认白名单内的类型

#### Scenario: 覆盖语义防误配

- **WHEN** 业务配置 `hc.redis.allowed-packages` 只写了一个业务包前缀（遗漏 `java.util.` 等基础包）
- **THEN** 默认白名单（含 `java.util.` 等）仍然生效，集合类型反序列化不因漏配而失败
