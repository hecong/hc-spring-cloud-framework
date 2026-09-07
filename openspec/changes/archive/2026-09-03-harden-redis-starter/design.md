## Context

`hc-redis-spring-boot-starter` 现状（见 proposal.md - Why）：
- `CustomGenericJackson2JsonRedisSerializer` 构造时 `allowIfBaseType(Object.class)`，任何 `@class` 都放行。
- `RedisCacheUtils.deleteByPrefix` 用 `keys(prefix + "*")`。
- `RedisSequenceGenerator` 的 INCR 与 EXPIRE 分离（`nextDaySeq`），无 TTL 兜底。
- `LockTemplate` 统一把异常包 `RuntimeException`。
- key 前缀 `seq:`/`seq:global:` 字面量散落在 `RedisSequenceGenerator`，`RedisKeyConstants` 目前仅有 `repeat:submit:`。

约束：不改 Redis 数据结构与既有 key（避免影响线上缓存与序列连续性）；模块不新增运行时依赖；改动需配合改进方案的发布顺序。

## Goals / Non-Goals

**Goals:**
- 反序列化白名单策略保持序列化端能力（不丢失容器泛型），仅在反序列化端拦截非法类型。
- `deleteByPrefix` 与序列取值的行为升级为可测试、可回归。
- key 收口遵循"先收口再演进"：存量前缀值不变，新前缀执行新规范。

**Non-Goals:**
- 不引入 ArchUnit/运行期硬编码 key 检测（按决策仅 CodeReview 约定）。
- 不把 `mq:idempotent:`（rocketmq 模块）、`excel:*`、`hc:satoken:` 前缀迁移纳入本 change——由对应模块后续认领，本 change 只确定规范并在仓库文档记录。
- 不为序列号引入 Redis Cluster/分片逻辑。

## Decisions

### D1. 反序列化防护：BasicPolymorphicTypeValidator + 保留容器多态

用 `BasicPolymorphicTypeValidator` 替换 `allowIfBaseType(Object.class)`，逐前缀 `allowIfSubType`；`DefaultTyping` 保持 `NON_FINAL`（否则 `List<User>` 等容器内类型的泛型信息丢失）。默认白名单沿用改进方案（`com.hc.framework.`、`com.hnhegui.`、`java.util.`、`java.lang.`、`java.time.`）。`hc.redis.allowed-packages` 为追加式 union。

- 备选 A：完全关闭 default typing——破坏所有泛型容器缓存，不可接受。
- 备选 B：Spring Security `AllowlistTypeResolver`——面向 ObjectMapper 单场景，且语义等价于自建 validator，收益低。
- 备选 C：`allowIfBaseType` 加前缀——baseType 与 subType 语义不同，P0-1 已实证漏洞路径，弃用。

实现要点：validator 与 mapper 构建结果缓存复用（每实例构建一次）；白名单空配置时保持默认值；配置校验：业务包前缀以 `.` 结尾归一化。

### D2. deleteByPrefix：SCAN + 分批 delete

`ScanOptions.scanOptions().match(prefix + "*").count(1000)` + `Cursor`，攒批 500 调一次 `redisTemplate.delete`，`try-with-resources` 保证游标关闭。语义尽力而为（spec 已定义）。

- 备选：`execute` pipeline 批量——收益有限且占用连接回调，保持简单。

### D3. 序列原子性：单 Lua 脚本

`DefaultRedisScript<Long>`：`INCR` 后若 `v==1` 则 `EXPIRE`。`nextDaySeq` key 形如 `seq:{biz}:{yyyyMMdd}` TTL 2 天（跨天兜底）；`nextPersistentSeq`（无 TTL 全局）保持原 INCR 语义，仅收口常量。

- 备选：`SET NX + EXPIRE + INCR` 三命令——非原子，弃。
- 兼容性：Lua 脚本不改变返回类型与已有 key 结构；`execute` 返回 null 时返回 0 的语义保持。

### D4. LockTemplate 异常语义

catch 顺序：`RuntimeException` → 原样抛；`InterruptedException` → `Thread.currentThread().interrupt()` + `LockException(INTERRUPTED)`；其余 → `LockException(EXECUTION_FAILED, e)`。finally 释放锁逻辑保持既有实现并核对确无泄漏。

- 行为变更点：业务方此前可能依赖"统一包 RuntimeException"，需在 README 发布说明标注（配合 P1-3 验证清单）。

### D5. key 收口策略

`RedisKeyConstants` 增加 `SEQ = "seq:"`、`SEQ_GLOBAL = "seq:global:"`（值与现有一致），`RedisSequenceGenerator` 改为常量引用。命名规范 `hc:{模块}:{业务}` 作为新增 key 的约定写入仓库 Redis 使用规范文档。

- 为什么不顺带改前缀值：`seq:` 改名会令当日已生成的序列号重置/跳变，属破坏性且无收益，故仅收口不改值。改进方案中"统一形如 hc:..." 的长远目标仅约束新增 key。

### D6. 测试策略

- `CustomGenericJackson2JsonRedisSerializer`：纯 JUnit5 单测（构造含 `javax.naming.Reference`、`TemplatesImpl` 类名字符串，断言抛 `InvalidTypeIdException`；白名单 DTO 往返）。
- `deleteByPrefix`/`RedisSequenceGenerator`/`LockTemplate`：Testcontainers Redis（改进方案测试表第 4 项）；若 CI 无法起 Docker，则备选 embedded redis（不影响运行时依赖，仅测试 scope）。

## Risks / Trade-offs

- [白名单收紧后，缓存内既有非白名单类型数据读取抛异常] → 按发布顺序执行（业务先配 `allowed-packages` → 升级框架 → 清理缓存）；README 顶部醒目标注。
- [默认白名单含 `java.util.`/`java.lang.`，理论上有新增 gadget 面] → 白名单子类型校验只允许前缀内具体类实例化，JDK 基础包内无已知反序列化 gadget；随版本安全评审维护默认列表。
- [P1-3 异常语义变化影响调用方 catch 逻辑] → 作为 BREAKING 在 release note 声明；回归测试覆盖业务异常原类型透传。
- [SCAN 分批删除在大 key 量下耗时变长] → 换取的 Redis 阻塞消除价值更高；量级提示记录于 Javadoc。
- [测试依赖 Testcontainers 需 Docker] → 提供 embedded redis 降级路径，仅在 test scope。

## Migration Plan

1. 框架版本发布前：业务项目先配置 `hc.redis.allowed-packages`（含自身实体/DTO 包前缀）。
2. 升级框架版本后：按发布窗口清理 Redis 缓存中可能含旧类型元数据的 key。
3. `LockTemplate` 调用方回归：锁内异常能被全局异常处理器识别（配合 web-starter 的 P1-4）。
4. 回滚策略：本 change 改动集中在序列化器/工具类/LockTemplate，回滚即还原版本；注意回滚后白名单失效需同步评估安全窗口。

## Open Questions

- rocketmq `mq:idempotent:`、excel `excel:*`、satoken `hc:satoken:` 等跨模块前缀的前缀迁移计划何时认领——不阻塞本 change（已在 Non-Goals 声明），后续由 rocketmq/excel 模块 change 跟进。
