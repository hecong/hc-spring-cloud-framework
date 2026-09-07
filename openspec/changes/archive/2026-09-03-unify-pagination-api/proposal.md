## Why

分页存在两套并存 API：`hc-mybatis-plus` 的 `PageParam`（含 Bean Validation 校验 + `toPage()`）已是统一入参形态，而 `hc-common` 的 `PageUtils` 仍被用于入参容器与原生 SQL 的 offset/总页数计算。两套并存导致业务写法分裂（`PageUtils.of(1,10).toMpPage()` vs `PageParam`），收口方向不明确，也阻碍后续演进。

## What Changes

- `PageUtils` 标记 `@Deprecated`（类与公开方法），Javadoc 声明：保留 **2 个大版本**（自当前 1.x 起，至 ≥3.0）后删除，并注明替代 API。
- 明确 `PageParam`（mybatis-plus，Bean Validation：页码 ≥1、每页 1–1000）为**唯一分页入参契约**；`PageData.of(IPage)` 为**唯一分页返回构造**。删除排期前 `PageUtils` 行为保持不变（offset/calcTotalPages/非法值修正仍可用，供原生 SQL 场景过渡）。
- README/使用文档给出迁移对照：Controller 入参 `PageUtils` → `PageParam`（配合 `@Valid`），分页查询 → `PageParam.toPage()`，返回组装 → `PageData.of(IPage)`。
- 框架/示例代码中 `PageUtils` 的使用点迁移到 `PageParam`（非破坏性，API 未删除）。

## Capabilities

### New Capabilities

- `mybatis/pagination`：统一分页入参/返回契约、校验规则与 PageUtils 的弃用排期。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：`hc-common-spring-boot-starter`（`util/PageUtils` 加 `@Deprecated` + Javadoc）、`hc-mybatis-plus-spring-boot-starter`（示例/使用点迁移至 `PageParam`）。
- **发布影响**：无运行时行为变更（deprecation 非 breaking）；README 声明删除排期（2 个大版本）。
- **验证**：全量编译（`@Deprecated` 不破坏调用方，仅为告警）+ 相关单测。
