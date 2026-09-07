# String Utils Specification

## Purpose

在 hc-common 提供稳定的字符串/集合工具契约：与 hutool 重复的工具方法通过委托复用既有实现，框架特色方法（脱敏/校验/格式化分割）保留；公开 API 长期稳定，调用方无需迁移。

## Requirements

### Requirement: 重复工具方法委托实现

系统提供的字符串/集合判空与转换工具方法（`isBlank`/`isNotBlank`/`isEmpty(Collection)`/`defaultIfBlank`/`nullToEmpty`/`capitalize`/`camelToUnderscore`/`truncate`）在 hutool 存在语义等价实现时，SHALL 委托给 hutool（`StrUtil`/`CollUtil`）执行，不再维护重复的自研逻辑；委托后行为（含空值/边界输入）SHALL 与原实现等价。

#### Scenario: 判空方法委托后行为等价

- **WHEN** 调用 `isBlank(null/""/"   ")`、`isNotBlank("x")`、`isEmpty(空集合/null)`
- **THEN** 返回结果与委托前完全一致（`true/false` 语义不变）

#### Scenario: 转换方法边界等价

- **WHEN** 调用 `camelToUnderscore("userId")`、`capitalize(null/"")`、`truncate("Hello World",8)`、`nullToEmpty(null)`、`defaultIfBlank("", "d")`
- **THEN** 返回内容与原实现一致（含 null/空输入原样返回等边界语义）

### Requirement: 特色方法保留

系统 SHALL 保留框架特色工具方法且行为不变：手机号/邮箱/身份证脱敏（`maskMobile`/`maskEmail`/`maskIdCard`）、合法性校验（`isMobile`/`isEmail`）、`splitAndTrim`；`format` 方法在 hutool `format` 行为不等价（如空参数、缺参、多参、花括号转义差异）时 SHALL 保留原实现。

#### Scenario: 脱敏与校验行为稳定

- **WHEN** 调用 `maskMobile("13812345678")`、`maskEmail("testuser@abc.com")`、`maskIdCard(11 位以下身份证)`、`isMobile`/`isEmail` 各类输入
- **THEN** 输出/判定与既有约定一致（参考类 Javadoc 示例）

#### Scenario: format 边界保持

- **WHEN** 调用 `format(template, args)` 且 args 为空数组、占位符多于/少于参数、模板含非 `{}` 花括号
- **THEN** 输出与原实现一致（若 hutool 语义存在差异，`format` 不委托）

### Requirement: 公开 API 稳定性

本收敛过程 SHALL 不删除、不重载变更任何 `StringUtils` 公开方法（签名与返回类型不变）。

#### Scenario: 调用方零迁移

- **WHEN** 既有业务代码调用 `StringUtils` 任意公开方法并编译
- **THEN** 编译通过且运行结果不变（无 API 删除或签名破坏）
