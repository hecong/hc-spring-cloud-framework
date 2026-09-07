## Why

`hc-redis-spring-boot-starter` 存在 5 类问题：①`CustomGenericJackson2JsonRedisSerializer` 通过 `allowIfBaseType(Object.class)` 放行所有类型多态反序列化，Redis 数据被篡改即可借 `@class` 触发 RCE；②`deleteByPrefix` 用 `keys()` 通配扫描，大 key 量下阻塞 Redis；③`RedisSequenceGenerator` 的 INCR 与 EXPIRE 分两步执行，中途故障会留下永不过期 key；④`LockTemplate` 把业务异常（如 `BusinessException`）包装成 `RuntimeException`，导致全局异常处理器无法识别而返回 500；⑤`seq:`、`mq:idempotent:` 等 key 前缀硬编码散落，无统一常量入口。

## What Changes

- **BREAKING** `CustomGenericJackson2JsonRedisSerializer`：`allowIfBaseType(Object.class)` 替换为 `BasicPolymorphicTypeValidator` 包白名单，默认放行 `com.hc.framework.`、`com.hnhegui.`、`java.util.`、`java.lang.`、`java.time.`；新增 `hc.redis.allowed-packages` 配置且为追加式（默认白名单 ∪ 业务配置），白名单外类型反序列化抛 `InvalidTypeIdException`。已缓存白名单外数据会在升级后读取失败，需按 README 顶部发布顺序清理缓存。
- `RedisCacheUtils.deleteByPrefix`：`keys(prefix+"*")` 改为 `SCAN` 游标 + 分批 `delete`（每批 ≤500），消除阻塞。
- `RedisSequenceGenerator`：`nextDaySeq`/`nextPersistentSeq` 自增与过期合并进一条 Lua 脚本（仅当值为 1 时设置 TTL），保证原子性与有界 TTL。
- **BREAKING** `LockTemplate`：执行回调时不再吞掉异常类型——`RuntimeException` 原样抛出（`BusinessException`/`IllegalArgumentException` 可被上层识别）；`InterruptedException` 恢复中断位并抛 `LockException(INTERRUPTED)`；其余异常包 `LockException(EXECUTION_FAILED)`。
- `RedisKeyConstants`：收口散落前缀（含 `RedisSequenceGenerator` 的 `seq:`、`seq:global:`，后续 rocketmq 的 `mq:idempotent:` 通过跨 change 协同统一规范为 `hc:{模块}:{业务}`），新增方法内一律引用常量；约定通过 CodeReview 执行（不引入 ArchUnit 依赖）。

## Capabilities

### New Capabilities

- `redis/serialization-security`：Redis 值多态反序列化的类型白名单约束与配置化追加，拒绝白名单外 `@class` 类型。
- `redis/batch-delete`：按前缀批量删除的 SCAN 非阻塞语义与分批删除结果一致性。
- `redis/atomic-sequence`：Redis 自增序列的原子性与 TTL 保证（Lua 单脚本）。
- `redis/distributed-lock`：锁内执行回调的异常语义（业务异常透传、中断恢复、失败包装）。
- `redis/key-namespacing`：Redis key 前缀的统一常量入口与命名规范。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：`hc-redis-spring-boot-starter`，受影响类：
  - `core/CustomGenericJackson2JsonRedisSerializer.java`
  - `util/RedisCacheUtils.java`（`deleteByPrefix`）
  - `util/RedisSequenceGenerator.java`
  - `lock/LockTemplate.java`
  - `constant/RedisKeyConstants.java`（扩充）
  - `RedisAutoConfiguration`（读取 `allowed-packages` 配置并注入）
- **依赖**：无新增运行时依赖；测试引入 Testcontainers/embedded redis（`LockTemplate`、并发取号）。
- **配置**：新增 `hc.redis.allowed-packages`（追加式，默认值见 spec）。
- **发布影响**：P0-1 与 P1-3 为破坏性行为变化——发布顺序（先配置白名单 → 升框架 → 清缓存）写入框架 README 顶部；调用 `LockTemplate` 的业务需回归捕获异常的类型语义。
