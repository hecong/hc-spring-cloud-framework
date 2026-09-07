## Why

工程化与 common 侧存在三处不一致：①`spring-boot-configuration-processor` 在模块间引入不一致（10 个模块中 8 个已引入，`hc-common`、`hc-excel` 缺失），配置提示能力不齐；②`hc-common` 的 `StringUtils` 中 `isBlank/isNotBlank/format/capitalize/camelToUnderscore/truncate` 等方法与已依赖的 hutool（hutool6 `StrUtil`）重复，形成两套并存实现，维护点分散；③代码包名与坐标不一致——模块 groupId 为 `com.hnhegui.framework`，而代码包为 `com.hc.framework.*`，命名割裂且与组织统一规范不符。

## What Changes

- **P1-7** 为缺失的 `hc-common`、`hc-excel` 两个模块 pom 补齐 `spring-boot-configuration-processor`（`optional`），使全部模块配置元数据生成能力一致；含 `@ConfigurationProperties` 的模块 `mvn compile` 后生成 `spring-configuration-metadata.json`，IDE 配置提示生效。
- **P2-6** `hc-common` `StringUtils` 瘦身：语义与 hutool6 完全一致的方法改为**委托** `StrUtil`/`CollUtil` 实现（`isBlank`/`isNotBlank`/`isEmpty(Collection)`/`defaultIfBlank`/`nullToEmpty`/`capitalize`/`camelToUnderscore`/`truncate`），**保留特色方法**（手机号/邮箱/身份证脱敏、isMobile/isEmail、splitAndTrim、format 视行为等价性验证结果决定），**不删除任何公开 API**，调用方零迁移。
- **P2-7** groupId 由 `com.hnhegui.framework` 统一为 **`com.hc.framework`**（与代码包名一致）：根 pom groupId、全部子模块 `parent` groupId、根 pom dependencyManagement 中自身 artifact 坐标、以及子 pom 中显式声明的框架模块依赖坐标（如 rocketmq 引用 redis/common/logging）同步替换；仓库内非 pom 的坐标引用（README/脚本）一并更新。外部依赖方迁移指引与"旧坐标停止发布新版本（保留过渡空壳按发布流程执行）"写入 README 发布说明。

## Capabilities

### New Capabilities

- `common/string-utils`：字符串/集合工具方法对外 API 的稳定契约与 hutool 委托的实现一致性。
- `build/coordinates`：仓库内全部模块的统一 Maven 坐标（groupId 与代码包对齐）及对外迁移指引。
- `build/module-metadata`：含配置属性的模块编译产物生成 Spring 配置元数据，供 IDE/文档工具消费。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：
  - `hc-common-spring-boot-starter`：`util/StringUtils`（委托化，无 API 删除）。
  - **全部 10 个模块 pom**：`spring-boot-configuration-processor`（optional）+ groupId 替换（根 pom 与 parent 引用）。
  - 仓库文档/脚本中的坐标引用。
- **依赖**：无新增运行时依赖（processor 为 optional 编译期依赖）。
- **构建/发布影响**：groupId 变更后，框架新版本将以 `com.hc.framework` 坐标发布；存量业务升级需切换依赖坐标（README 给出对照与过渡说明）。本 change 与并行其他 change 的产物将在同一框架版本中生效。
- **验证**：`mvn help:evaluate -Dexpression=project.groupId` 断言新坐标；编译后检查各模块 `META-INF/spring-configuration-metadata.json`；StringUtils 行为等价测试。
