# Atomic Sequence Specification

## Purpose

为基于 Redis 的自增序列（当日序列号、全局序列号）提供原子且生命周期可控的取值能力：自增与首次过期设置在同一原子操作内完成，并发取号不重复，且不会因执行中断残留永不过期的 key。

## Requirements

### Requirement: 自增与过期原子性

系统生成带 TTL 的序列值时，SHALL 在同一原子操作（如单条 Lua 脚本）内完成自增与过期设置：仅当 key 首次创建（值为 1）时设置 TTL，后续自增不得重置已存在的 TTL。

#### Scenario: 首次取号设置过期

- **WHEN** 对不存在的当日序列 key 首次取号
- **THEN** 返回 1，且该 key 带有预设 TTL（有效期内不会因后续自增被反复延长/清除）

#### Scenario: 已有 key 自增不重置 TTL

- **WHEN** 对已存在的序列 key 再次取号
- **THEN** 返回递增后的值，且该 key 的剩余过期时间不被刷新（保持首次创建时的时间基准）

#### Scenario: 无残留无过期 key

- **WHEN** 并发/串行取号过程中任意时刻发生中断或异常
- **THEN** 不会留下无 TTL 的序列 key；已创建的 key 要么带 TTL，要么未被创建

### Requirement: 并发取号不重复

系统 SHALL 保证多线程/多实例并发取号时返回的序列号互不重复，且单调递增。

#### Scenario: 并发取号唯一性

- **WHEN** 100 个线程同时取同一业务 key 的序列号
- **THEN** 返回的 100 个值互不重复

### Requirement: 失败返回值语义

当 Redis 执行异常导致取号结果缺失时，系统 SHALL 返回明确的失败语义（如 0），供调用方按不可用处理，不得返回看似有效但可能重复的值。

#### Scenario: Redis 不可用时的返回值

- **WHEN** 底层 Redis 调用未返回结果
- **THEN** 调用方得到 0 或明确异常，而不会得到可能重复的序列值
