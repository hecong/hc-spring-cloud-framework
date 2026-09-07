## Purpose

为 Redis key 建立统一的前缀常量入口与命名规范，收敛散落的硬编码 key 字符串，使 key 可治理、可审计、可前缀清理。

## ADDED Requirements

### Requirement: key 前缀常量入口

系统在 `hc-redis-spring-boot-starter` 内提供的所有 Redis key SHALL 通过统一常量类（`RedisKeyConstants`）引用其前缀，业务代码与框架代码中 SHALL 不得内联 Redis key 前缀字面量。

#### Scenario: 散落前缀被收口

- **WHEN** 检索框架内 Redis key 的使用处（如自增序列生成）
- **THEN** 前缀均通过常量类引用，无直接内联的 key 前缀字符串

#### Scenario: 序列前缀值保持不变

- **WHEN** 收口已上线的序列 key 前缀（如 `seq:`、`seq:global:`）到常量类
- **THEN** 常量值与原字面量一致，已生成的历史 key 不受影响（不因收口改动实际存储 key）

### Requirement: 新增 key 前缀命名规范

系统新引入的 Redis key 前缀 SHALL 遵循 `hc:{模块}:{业务}` 的命名规范，其中模块与业务均使用小写字母与连字符。

#### Scenario: 新前缀符合规范

- **WHEN** 框架内新加入一个 Redis key 前缀（如某模块幂等键）
- **THEN** 前缀形如 `hc:模块:业务:`，并经常量类暴露

### Requirement: 代码评审约定约束

硬编码 key 字符串的约束以 CodeReview 契约为准：本 change 不引入强制校验工具依赖；仓库文档中 SHALL 记录该约定，作为后续评审检查项。

#### Scenario: 约定可被评审引用

- **WHEN** 仓库文档描述 Redis key 使用规范
- **THEN** 文档明确"禁止在业务代码内联 Redis key 字符串，一律走常量类"的约定
