## 1. 反序列化白名单（serialization-security）

- [x] 1.1 改造 `CustomGenericJackson2JsonRedisSerializer`：构造注入 `allowedPackages`，用 `BasicPolymorphicTypeValidator` 逐前缀 `allowIfSubType`，保留 `DefaultTyping.NON_FINAL` typing；默认白名单为 `com.hc.framework.`、`com.hnhegui.`、`java.util.`、`java.lang.`、`java.time.`；验证含指向 `javax.naming.Reference` / `TemplatesImpl` 类型 id 的 JSON 反序列化抛 `InvalidTypeIdException`（单测通过）
- [x] 1.2 在 Redis 自动配置中读取 `hc.redis.allowed-packages`，与默认白名单取并集后传入序列化器（RedisTemplate 与 Spring Cache 共用同一实例）；验证：配置了业务包后该包 DTO 往返一致、漏配 `java.util.` 时集合类型仍可反序列化（单测覆盖两个场景）
- [x] 1.3 补充白名单内 DTO（嵌套集合 + `LocalDateTime`）往返单测；验证类型元数据（类名等）在序列化-反序列化后不丢失

## 2. 前缀删除 SCAN 化（batch-delete）

- [x] 2.1 重写 `RedisCacheUtils.deleteByPrefix`：`ScanOptions(count=1000)` 游标遍历 + 攒批 ≤500 批量 `del`，`try-with-resources` 关闭游标；验证：真实 Redis 写入 2000 个前缀 key 后删除，剩余匹配数为 0
- [x] 2.2 验证幂等与并发容忍：对无匹配前缀重复删除不抛异常且返回 0；遍历期间并发写入的 key 不导致删除中断

## 3. 序列原子性（atomic-sequence）

- [x] 3.1 `RedisSequenceGenerator` 新增 `DefaultRedisScript<Long>`：`INCR` 后当且仅当值为 1 时 `EXPIRE`（TTL 2 天），`nextDaySeq` 走脚本执行；`nextPersistentSeq`（无 TTL 全局）保持原 INCR 语义、仅收口常量（与 design D3 对齐，避免给永久 key 附加 TTL）；验证：新 key 首次取号后 `getExpire > 0`，已有 key 再取号不刷新剩余 TTL
- [x] 3.2 并发测试：100 线程对同一业务 key 取号，断言 100 个值互不重复（真实 Redis 用例通过）
- [x] 3.3 覆盖 Redis 调用失败返回 null 的语义：断言取号结果为 0 而非空指针或重复值（单测 + 文档化）

## 4. 锁异常语义（distributed-lock）

- [x] 4.1 重构 `LockTemplate` 回调异常处理：`RuntimeException` 原样抛出；`InterruptedException` 先恢复中断位再抛 `LockException(LOCK_INTERRUPTED)`；其余异常包 `LockException(LOCK_EXECUTION_FAILED)`（cause 保留）；验证：锁内抛 `BusinessException` 时外部捕获到原类型且消息/错误码不变
- [x] 4.2 验证中断场景：回调模拟 `InterruptedException`，断言中断标志已恢复且收到 `LOCK_INTERRUPTED` 类型锁异常
- [x] 4.3 回归验证锁释放：回调异常/中断两种路径后锁均被释放，后续线程可再获取同一把锁（实现中发现：中断位置位时 Redisson 同步命令被中断标志打断会掩盖异常并阻塞释放，已修复——释放前临时清除中断位、释放后恢复）

## 5. key 收口（key-namespacing）

- [x] 5.1 `RedisKeyConstants` 增加 `SEQ`、`SEQ_GLOBAL` 常量（值与原字面量一致），`RedisSequenceGenerator` 全部改用常量引用；验证：模块编译通过且 `main` 源码无内联 `seq:`/`seq:global:` 字面量
- [x] 5.2 在 Redis 使用规范（模块 README）记录命名约定与 CodeReview 检查项（新前缀遵循 `hc:{模块}:{业务}`、禁止硬编码 key 字符串）；验证：文档可被评审引用

## 6. 测试基建与全量验证

- [x] 6.1 为模块引入集成测试基建（按实现决策替代 Testcontainers/embedded redis 方案）：真实本地 Redis（默认 127.0.0.1:6379，可 `-Dredis.host/-Dredis.port` 覆盖），不可达时以 assumption 跳过、无 Redis 的 CI 仍为绿色；用例全部使用随机前缀 key 并测后清理，测试依赖均仅 test scope
- [x] 6.2 README 顶部加入发布顺序说明：业务先配 `allowed-packages` → 升级框架 → 清理含旧类型元数据的缓存；标注 P0-1（白名单收紧）与 P1-3（锁异常语义/序列取号方式变更）为 BREAKING；验证：README 发布说明存在且步骤完整
- [x] 6.3 运行 `mvn verify`（全量），验证本 change 相关单测/集成测试全绿且无回归
