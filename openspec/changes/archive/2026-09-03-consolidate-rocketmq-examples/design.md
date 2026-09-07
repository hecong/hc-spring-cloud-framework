## Context

见 proposal.md - Why 与对应 spec。代码基线（已核对）：
- 示例类现状（全部在 `src/test/java/.../example`）：`MqTestController`、`NormalMessageConsumer`、`TestMessageDTO`、`TransactionMessageChecker`。
- `src/main` 无示例类；main 含配置/实体/发送器/消费者基类等生产代码，无指向 test 的引用。
- pom 测试依赖：`spring-boot-starter-test`；模块 main 依赖 rocketmq-v5-client-spring-boot-starter、hc-common（非 optional）、hc-redis/hc-logging（optional）。`MqTestController` 若声明 `@RestController` 则需要 `spring-web` 出现在 test classpath（starter-test 不传递 spring-webmvc）——**待 `test-compile` 验证**，缺失则补 test scope 的 `spring-boot-starter-web`（仅测试）。

## Goals / Non-Goals

**Goals:**
- 把"示例留在 test 集"固化为可验证约束（构建核验 + 文档约定）。
- 补齐本地运行示例的操作指引。

**Non-Goals:**
- 不搬移示例代码（现状已合规）；不新增独立 example 模块。
- 不改 `mq:idempotent:` 幂等键前缀值（幂等窗口失效风险，独立评估）。
- 不做 RocketMQ 集成测试基建（真实 Broker 联调不在本 change）。

## Decisions

### D1. 示例边界核验

示例边界的核验通过两种手段固化：
1. `mvn package`/`jar tf` 断言产物无 `com/hc/framework/rocketmq/example/` 类（作为任务手动作业或归档检查点）。
2. `src/main` 全量检索 `example.` 引用为空的检查。
不新增测试框架（spring-boot-starter-test 自带能力不覆盖 jar 内容断言，故以文档化检查点 + 任务验证为主；如需自动化可后续增加 maven-enforcer 规则，列为 Open Question）。

### D2. 示例可编译性

先执行 `test-compile` 实测。若 `MqTestController` 因缺少 `spring-web`（jakarta.servlet 注解 / RequestMapping）编译失败，则在 pom 增加 test scope `spring-boot-starter-web`（不影响 main 依赖面）。若编译已通过，则无需改 pom。

### D3. 运行指引

README RocketMQ 章节补充：
- 依赖基础设施：RocketMQ 5.x（NameServer + Broker）或 Proxy 端点。
- 关键配置示例：`rocketmq.name-server`（或 proxy）、示例 Topic/Group、`hc.rocketmq.*` 自有配置。
- 示例入口：`example` 下类清单 + 各自用途（普通消息/事务消息/批量/延时/顺序）。
- 通过 Spring Boot test 上下文运行示例的方式说明。

## Risks / Trade-offs

- [示例未来被写回 main 导致业务扫描误加载] → 文档检查项 + jar 断言检查点防回归（D1）。
- [补 test 依赖 `spring-boot-starter-web` 增大测试上下文] → 仅 test scope；若示例可编译则不改 pom。
- [幂等键前缀与 hc 规范不一致未在本 change 处理] → 已在 Open Questions/c1 记录，单独评估后处理。

## Migration Plan

无运行时迁移；改动与文档随框架下一版本发布即可。

## Open Questions

- 示例边界核验是否需自动化（maven-enforcer 或 archunit）——当前以文档检查点 + 手动断言为准，量级小不值得引入新插件；如后续频繁回归再评估。
