## Purpose

保证 RocketMQ 框架示例（Controller/消费者等）只存在于测试源码集，主代码与发布产物保持纯净，不被业务项目启动扫描误加载；同时提供本地运行示例的可执行指引。

## ADDED Requirements

### Requirement: 示例代码位于测试源码集

`hc-rocketmq` 模块的示例类（如 `MqTestController`、`NormalMessageConsumer` 等）SHALL 位于 `src/test` 源码集内，不得放入 `src/main`；`src/main` 的发布产物 SHALL 不包含示例类。

#### Scenario: 发布产物不含示例类

- **WHEN** 检查 `mvn package` 生成的 starter jar 内容
- **THEN** 包内不存在 `example` 包下的示例类（示例类仅存在于 `src/test`）

#### Scenario: 主代码不引用示例

- **WHEN** 检索 `src/main` 中指向 `src/test` 示例类的引用
- **THEN** 不存在（main 不反向依赖测试示例，避免循环与误加载）

### Requirement: 示例可编译可运行前提

示例类 SHALL 满足可编译与可运行的前提：`src/test` 的依赖（如 `spring-web`/web 支持）齐备；执行 `test-compile` SHALL 成功。

#### Scenario: 示例测试源码可编译

- **WHEN** 执行 `mvn -pl hc-rocketmq-spring-boot-starter test-compile`
- **THEN** 编译成功（示例所需的测试类路径依赖已具备）

### Requirement: 本地运行指引

框架文档 SHALL 提供 RocketMQ 本地示例的运行指引：所需基础设施（NameServer/Broker/Proxy）与地址配置方式、示例入口类及收发消息步骤，使开发者可按文档启动示例。

#### Scenario: 开发者可按文档运行示例

- **WHEN** 开发者按 README 指引配置 RocketMQ 端点并启动示例入口
- **THEN** 步骤明确、可完成消息收发（无需阅读源码推断配置）
