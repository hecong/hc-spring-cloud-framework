## Context

见 proposal.md - Why 与对应 spec。基线（已核对）：
- 根 pom：`groupId=com.hnhegui.framework`，`version=1.0-SNAPSHOT`，10 个 modules，`dependencyManagement` 含 10 个本仓库模块坐标与 `transmittable-thread-local 2.14.5`、`hutool-all`（org.dromara.hutool，hutool6）、Jackson 3（tools.jackson）等。
- 子模块 pom：自身不写 groupId，仅通过 `parent`（com.hnhegui.framework）继承；`hc-common` 已依赖 `hutool-all` 与 `transmittable-thread-local`（非 optional、版本托管于根 pom）。
- processor 引入现状（已逐模块核对 pom）：10 个模块中 8 个已含 `spring-boot-configuration-processor`（logging/mybatis-plus/oss/redis/rocketmq/satoken-gateway/satoken/web），**缺失 `hc-common`、`hc-excel`**。
- `hc-common.util.StringUtils` 公开方法（已核对）：判空 `isBlank/isNotBlank/isEmpty(Collection)/isEmpty(Map)`，默认值 `defaultIfBlank/nullToEmpty`，转换 `format/camelToUnderscore/capitalize/truncate/splitAndTrim`，特色 `maskMobile/maskEmail/maskIdCard/isMobile/isEmail`。

## Goals / Non-Goals

**Goals:**
- StringUtils 委托化但**对外零迁移**（不删 API、不换签名）。
- 工程化改动机械化、可脚本化验证（坐标全局可断言）。
- 全部改动与并行模块 change 同版本发布，不产生中间不稳定态。

**Non-Goals:**
- 不重命名代码包 `com.hc.framework`（包名已是规范目标）。
- 不执行"旧坐标空壳 artifact"的实际发布动作（属发布流程，仓库内仅出迁移指引）。
- 不做 StringUtils 的整体重构（方法职责重划、去 static 等）。

## Decisions

### D1. StringUtils 委托策略（P2-6）

对 hutool6 存在**语义等价**实现的方法改为一行委托：
- `isBlank`→`StrUtil.isBlank`、`isNotBlank`→`StrUtil.isNotBlank`
- `isEmpty(Collection)`→`CollUtil.isEmpty`、`isEmpty(Map)`→`MapUtil.isEmpty`
- `nullToEmpty`→`StrUtil.nullToEmpty`、`defaultIfBlank`→`StrUtil.ifBlank`（返回按 String 收窄）
- `capitalize`→`StrUtil.upperFirst`（收窄 String）、`camelToUnderscore`→`StrUtil.toUnderlineCase`
- `truncate`→`StrUtil.maxLength`（若省略号/长度语义不一致则保留原实现，见 tasks 行为对比）

**保留原实现**（hutool 语义不等价或属框架特色）：
- 脱敏/校验族 `maskMobile/maskEmail/maskIdCard/isMobile/isEmail`、`splitAndTrim`。
- `format`：hutool6 `StrUtil.format` 对空参、缺参/多参、转义等边界行为需先做行为对比，**仅当全部等价才委托，否则保留原实现**（spec 已留口）。

委托注意：返回类型收窄处做 `toString()`/cast 保持 `String` 契约；避免委托引入 NPE（如 hutool 个别方法对 null 语义差异）。
风险兜底：任何方法的行为对比测试失败 → 该方法退回原实现并在 Javadoc 注明原因（保留两套不在本 change 目标内，但安全优先）。

### D2. configuration-processor（P1-7）

已核对 pom：8 个模块已引入（见 Context），仅 `hc-common`、`hc-excel` 缺失。为这两个模块 pom 加入：
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-configuration-processor</artifactId>
  <optional>true</optional>
</dependency>
```
processor 是**编译期 annotation processor**，随 Spring Boot BOM 管理版本。含配置类的模块自动产出 metadata；若 `hc-common` 不含 `@ConfigurationProperties` 类，产物无对应 metadata 亦不影响构建（补齐仅为构建声明一致性，改进方案要求全部模块统一）。
风险：极少数情况 processor 与 Lombok annotation processing 顺序冲突（hc-common 用 Lombok @Data 且为 provided）——编译验证覆盖；若冲突按"在含 Lombok 模块配置 annotationProcessorPaths"处理。

### D3. groupId 统一（P2-7）

替换清单：
1. 根 pom `groupId`（1 处）。
2. 全部子模块 `parent` 块 groupId（10 处）。
3. 子 pom 中**显式声明**的框架模块依赖 groupId（存在显式 groupId 引用的模块，如 rocketmq 引用 redis/common/logging、mybatis-plus 引用 common 等）。
4. 根 pom `dependencyManagement` 中本仓库自身坐标。
5. 非 pom 引用（README 等）——tasks 检索后同步（README 中"新→旧对照说明"除外）。
执行方式：脚本化全局替换 + `mvn help:evaluate` 断言；不触碰外部依赖坐标。

对外迁移：README 发布说明新增坐标对照表；"旧坐标保留空壳 artifact 过渡"作为发布选项写入说明（由发布流程决定是否发布空壳，仓库内不新增空壳模块）。

### D4. 与并行 change 的关系

本 change 只改坐标/pom/`StringUtils`，与 c1~c4/c6/c7 无文件冲突（pom 修改可能与其他 change 的 pom 改动并存——若同批发布，CI 在合并后统一验证 `mvn verify` 全量即可）。

## Risks / Trade-offs

- [hutool6 个别方法边界行为与原实现不一致导致回归] → 行为对比测试为强制关卡；失败即不委托（D1 兜底）。
- [`camelToUnderscore`/`truncate` 细节差异（如字符集、省略号）] → tasks 单测比对；不确定则保留。
- [groupId 全局替换遗漏非 pom 引用] → tasks 全仓文本检索 `com.hnhegui.framework` 兜底（除迁移对照说明）。
- [processor 与 JDK21/annotation processing 兼容] → 编译验证；异常时按 annotationProcessorPaths 显式配置。
- [业务侧旧坐标断供窗口] → 发布说明给出对照与过渡选项，控制发布窗口。

## Migration Plan

1. 仓库内完成三块改动并全量 `mvn verify` 通过。
2. 版本发布时按 README 发布说明执行：新坐标发布 + （可选）旧坐标空壳。
3. 业务升级：切换依赖坐标；IDE 导入配置类自动提示。

## Open Questions

- 暂无。
