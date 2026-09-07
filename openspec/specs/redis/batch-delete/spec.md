# Batch Delete Specification

## Purpose

按 key 前缀批量删除缓存数据时使用非阻塞的游标遍历（SCAN）与分批删除，避免 `KEYS` 全量扫描阻塞 Redis 单线程，同时保持删除语义与可重入性。

## Requirements

### Requirement: 前缀删除非阻塞

系统按指定前缀删除 Redis key 时，SHALL 通过游标迭代分批完成，不得使用会阻塞 Redis 的全量扫描命令（如 `KEYS`）。单次扫描游标数量 SHALL 有界（如 1000），单次删除批量 SHALL 有界（如 ≤500），避免单命令过大。

#### Scenario: 大批量 key 删除不阻塞

- **WHEN** 缓存中存在数千个匹配指定前缀的 key，调用按前缀删除
- **THEN** 删除以多轮 SCAN + 分批 DELETE 完成，过程中无全量阻塞式扫描，最终所有匹配 key 被删除

#### Scenario: 方法可重入

- **WHEN** 再次对同一前缀（已无匹配 key）调用删除
- **THEN** 正常返回且不抛出异常（删除是幂等操作）

### Requirement: 删除语义与并发容忍

系统按前缀删除 SHALL 采用尽力而为的最终一致语义：对遍历期间新增/删除的 key 不做强一致承诺，删除过程不因个别 key 消失或中途写入而失败中断。

#### Scenario: 遍历期间 key 变化

- **WHEN** 删除遍历过程中有匹配 key 被其他客户端删除或新增
- **THEN** 删除操作仍正常完成，仅对已存在且被遍历到的 key 执行删除，不抛出非预期异常
