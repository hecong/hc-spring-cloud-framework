## Context

见 proposal.md - Why 与对应 spec。代码基线（已核对）：
- `PageParam`（mybatis-plus starter，`model/PageParam`）：`pageNum`（`@NotNull/@Min(1)`，默认 `SystemConstants.DEFAULT_PAGE_NUM`）、`pageSize`（`@NotNull/@Min(1)/@Max(1000)`，默认 `DEFAULT_PAGE_SIZE`）+ `toPage()` + `of(...)`。已是统一入参形态。
- `PageData<T>`（mybatis-plus starter，`model/PageData`）：`of(IPage<T>)` 计算 totalPage/hasNext。
- `PageUtils`（hc-common，`util/PageUtils`）：不可变思想但 `@Data` 生成 public setter（未收口）；职责 = 入参容器 + `getOffset()` + `calcTotalPages` + 非法值修正（pageNum≥1、pageSize≤`MAX_PAGE_SIZE=1000`）+ isFirstPage/isLastPage + `defaultPage()`。无 MyBatis 依赖（hc-common 不依赖 mybatis）。
- hc-mybatis-plus 模块依赖 hc-common（引用 `SystemConstants`）。

## Goals / Non-Goals

**Goals:**
- 确立 PageParam/PageData 为唯一契约，PageUtils 进入可预期的弃用周期。
- deprecation 阶段零破坏：行为与签名不变。

**Non-Goals:**
- 本 change **不删除** PageUtils（删除在 ≥2 个大版本后执行，另立 change）。
- 不为 PageParam 增加 offset 等原生 SQL 辅助方法（原生 SQL 过渡期仍可用 PageUtils；删除前再规划能力迁移，见 Open Questions）。
- 不改 `SystemConstants` 中分页常量与校验上限。

## Decisions

### D1. 弃用标记范围

`PageUtils` 类及其**全部公开方法**（of/defaultPage/getOffset/calcTotalPages/isFirstPage/isLastPage + 实例 getter）标记 `@Deprecated`。类 Javadoc 顶部增加弃用说明块：`@since`（当前 1.x）、替代指引（入参用 `PageParam` + `toPage()`、返回用 `PageData.of(IPage)`）、删除排期（自本版本起 2 个大版本后删除）。

注意：`@Deprecated` 对既有调用方仅产生编译告警，不破坏编译；框架内自身方法互调（如 calcTotalPages(long) 调静态重载）若同属已弃用方法，使用 `@SuppressWarnings("deprecation")` 局部消除或接受告警（保留在类内调用处，不在 Javadoc 里写实现细节）。

### D2. 示例与框架使用点迁移

检索框架/示例内 `PageUtils` 使用点：Controller 入参与分页查询示例改用 `PageParam`（`@Valid`）+ `toPage()`，返回组装改 `PageData.of(IPage)`。迁移仅改示例代码，不触碰业务（框架无业务调用方）。验证迁移点编译通过且 `@Deprecated` 告警不在示例中出现。

### D3. 文档与排期

README 发布说明新增"分页 API 收敛"段：新旧写法对照（含原生 SQL 用户指引——删除前过渡期内继续使用 PageUtils，后续删除时由迁移 change 提供 offset 计算替代）。

## Risks / Trade-offs

- [`@Data` 生成 public setter 与"不可变"注释相悖，弃用期可能继续被业务用作可变容器] → 弃用说明强调不可变用法；收口 setter 属行为变更，不在本 change。
- [删除排期依赖版本节奏承诺] → README 排期即契约，2 个大版本（≥3.0）为计划节点，删除 change 另行 propose。
- [原生 SQL 用户依赖 PageUtils 的 offset/修正能力] → 过渡期保留可用，README 明示未来替代路径（分页插件 offset 由 MP 提供；纯手写分页可 `(pageNum-1)*pageSize`）。

## Migration Plan

1. 标记 `@Deprecated` + Javadoc（本 change）。
2. 业务侧按 README 对照在 2 个大版本内完成 `PageParam` 迁移。
3. 排期到点后单独 change 执行删除，并在删除 change 内补齐原生 SQL 场景的 offset 计算迁移路径。

## Open Questions

- PageUtils 删除时，`getOffset()`/`calcTotalPages` 能力是否迁入 `PageParam`（增实例方法）或交给使用方自行计算——留待删除 change 决策（本 change 排期说明中已预留）。
