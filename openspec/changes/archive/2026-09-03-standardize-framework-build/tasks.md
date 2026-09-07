## 1. StringUtils 委托化（string-utils）

- [x] 1.1 行为对比先行：临时探针 `StringUtilsDelegationProbeTest`（决策后已删除）对 hutool6 全向量对比（null/空/典型空白/Unicode 特殊空白/连续大写缩写/多参缺参/花括号与反斜杠转义/长度边界）。**委托结论（已写入 `StringUtilsTest` 与各方法 Javadoc）**：
  - **委托**：`isBlank/isNotBlank`（StrUtil）——NBSP(U+00A0)/BOM(U+FEFF) 判 blank 为 hutool6 超集对齐并记录、ZWSP(U+200B) 与旧 `String.isBlank` 一致；`isEmpty(Collection)`（CollUtil）、`isEmpty(Map)`（MapUtil）、`nullToEmpty`（StrUtil.emptyIfNull）、`defaultIfBlank`（StrUtil.defaultIfBlank）、`capitalize`（StrUtil.upperFirst）在覆盖向量上全等价；
  - **保留**：`format`（`\{}` 反斜杠转义、null 模板语义不同）、`camelToUnderscore`（hutool6 `toUnderlineCase` 对连续大写缩写保留大写，如 `URLValue→URL_value`，与全小写契约不符）、`truncate`（hutool6 无 `maxLength`，`limitLength` 要求 maxLength>0，与 maxLength=0 等契约不符）；验证：探针差异报告已输出并评估，最终 `StringUtilsTest`（15 条）锁定委托后契约
- [x] 1.2 已委托方法改为一行委托并按 `String` 收窄返回（`defaultIfBlank/capitalize` 入参与返回均 String 静态推断）；保留方法未动；验证：`mvn -pl hc-common-spring-boot-starter test` 全绿（36 条 = IpUtils 11 + PageUtils 10 + StringUtils 15）
- [x] 1.3 特色方法（脱敏/校验/splitAndTrim）保留原实现；全部委托方法 Javadoc 标注委托目标与空白边界；公开 API 与签名零变化；验证：全量 `mvn verify` 通过（含各模块既有 StringUtils 调用方编译）

## 2. configuration-processor（module-metadata）

- [x] 2.1 核对现状：8 模块已含，缺失 `hc-common`、`hc-excel` 已补齐（optional，随 Boot BOM 管版本）；验证：`mvn -q -pl hc-common,hc-excel clean compile` 通过
- [x] 2.2 抽查 metadata：excel `hc.excel`(10 条)、web `hc.web`(9)、oss `hc.oss`(19)、redis `hc.redis`+第三方 `custom.cache`(2)、logging `hc.logging`(11)——均含 `hc.*` 前缀条目；hc-common 无配置类，产物为空不影响构建（符合设计说明）；验证：5 模块 metadata JSON 已解析抽查
- [x] 2.3 processor 与 Lombok 无冲突（common/excel 均使用 Lombok @Data provided，编译全绿），无需 annotationProcessorPaths

## 3. groupId 统一（coordinates）

- [x] 3.1 替换根 pom groupId、10 个子模块 parent groupId、子 pom 显式框架依赖（web→common、rocketmq→redis/common/logging、satoken 系列等）与根 pom dependencyManagement 中本仓库坐标：`com.hnhegui.framework`→`com.hc.framework`（11 个 pom 全部）；验证：`mvn help:evaluate` root/web/rocketmq 输出 `com.hc.framework`
- [x] 3.2 全仓文本检索非 pom 引用并同步：10 处 README 依赖坐标、CLAUDE.md 架构描述已更新；剩余引用仅为合法对照/历史说明（根 README 迁移对照表、改进方案 P2-7 决策陈述、openspec change 文档）；`.idea/workspace.xml`（未跟踪 IDE 缓存）未改；验证：检索结果仅剩上述合法引用
- [x] 3.3 全量 `mvn verify` **BUILD SUCCESS**（11 模块，模块内互引经新坐标 dependencyManagement 正常解析）

## 4. 迁移文档

- [x] 4.1 根 `README.md`（新建）发布说明含：①坐标对照表与切换步骤、旧坐标停止发布说明与空壳 artifact 过渡选项；②configuration-processor 统一后 IDE 配置补全/跳转说明；③StringUtils 委托化声明（API 不变、委托/保留清单、空白边界）；验证：README 章节存在、步骤可执行；另 8 个模块 README 依赖示例坐标已同步为 `com.hc.framework`
