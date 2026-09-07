# Distributed Lock Specification

## Purpose

定义基于 Redis 的分布式锁在执行持锁回调时的异常语义：业务异常保持原类型透传以被全局异常处理器识别，线程中断被妥善恢复，锁资源在所有路径上可靠释放。

## Requirements

### Requirement: 业务异常原样透传

系统执行持锁回调时，若回调抛出运行时异常（如业务异常、参数校验异常），SHALL 以原始异常类型与消息向外抛出，不得包装成其他类型。系统级异常处理器据此能按原始错误码返回。

#### Scenario: 锁内业务异常透传

- **WHEN** 持锁回调内抛出业务异常（如 `BusinessException`，携带业务错误码与消息）
- **THEN** 调用方捕获到的仍是原异常类型，错误码与消息不变，外层异常处理器可识别并映射到业务错误码响应

#### Scenario: 运行时参数异常透传

- **WHEN** 持锁回调内抛出 `IllegalArgumentException` 等运行时异常
- **THEN** 调用方捕获到的仍是原类型，未被包装

### Requirement: 中断与异常分类语义

系统执行持锁回调时 SHALL 区分三类结果：线程中断（恢复中断标志并以中断语义异常报告）、业务/运行时异常（透传）、其他异常（以执行失败语义包装）。

#### Scenario: 回调抛受检异常

- **WHEN** 持锁回调内抛出受检异常或其他未知异常
- **THEN** 调用方收到锁执行失败语义的异常（如 `LockException(EXECUTION_FAILED)`），原始异常作为 cause 保留

#### Scenario: 线程中断被恢复

- **WHEN** 持锁回调执行期间当前线程被中断（回调内抛出或收到 `InterruptedException`）
- **THEN** 当前线程的中断标志被重新置位，调用方收到中断语义异常（如 `LockException(INTERRUPTED)`），不会被静默吞掉

### Requirement: 锁资源可靠释放

系统执行持锁回调时，无论回调成功、抛异常还是线程中断，SHALL 在退出时释放锁资源，不得因异常路径泄漏锁。

#### Scenario: 异常后锁已释放

- **WHEN** 持锁回调抛出任意异常
- **THEN** 锁在异常传播前被释放，其他线程可正常获取同一把锁
