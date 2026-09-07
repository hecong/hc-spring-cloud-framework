## Why

改进方案 P2-8 指出 `hc-rocketmq` 的示例类（`MqTestController`、`NormalMessageConsumer` 等）位于主代码包，会被业务项目启动时组件扫描误加载（拉起不需要的消费者/Web 组件）。**已核对源码：示例类现已全部位于 `src/test`**（`example` 包），main 无示例残留——该问题在现状中已消除。剩余缺口：无正式核验手段防止回归（未来开发者把示例写回 main 时不告警）、本地运行示例无操作指引。

## What Changes

- 建立示例边界核验：确认 main 源码与发布产物均不含 `example` 示例类；作为常规检查项写入模块测试或文档检查清单，防止示例回归 `src/main`。
- 核验示例类在 `src/test` 下**可编译/可运行**的前提依赖齐备；若缺失（如 `@RestController` 需要 `spring-web` 出现在 test classpath），补齐 test scope 依赖（不进入 main 依赖）。
- README 补充"本地运行 RocketMQ 示例"指引：需要的基础设施（NameServer/Broker 或 proxy 端点）与配置项（name-server/proxy 地址、Group、Topic）、示例入口类清单。
- 幂等键前缀（`mq:idempotent:`）维持现状（已有模块常量 `IDEMPOTENT_KEY_PREFIX`），值与"hc 前缀规范"的差异沿用 c1 `key-namespacing` 的 Open Questions 记录，不在本 change 改动（幂等键改名存在短暂失效窗口，需单独评估）。

## Capabilities

### New Capabilities

- `rocketmq/example-scope`：框架示例代码的源码集边界（测试集内）、发布产物纯净性与本地运行指引。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：`hc-rocketmq-spring-boot-starter`
  - 可能补 `src/test` 的 spring-web 等 test 依赖（若 `test-compile` 验证发现缺失）。
  - 示例类当前已在 `src/test`，本 change 不搬移代码。
- **文档**：README 本地运行示例指引 + 示例边界检查项说明。
- **验证**：`mvn -pl hc-rocketmq-spring-boot-starter test-compile` 通过；`mvn package` 产物不含 `example` 类。
- **发布影响**：无运行时行为变更。
