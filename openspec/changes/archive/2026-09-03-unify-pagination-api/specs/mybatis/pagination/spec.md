## Purpose

明确统一的分页入参与返回契约，规范 `PageUtils` 弃用路径与删除排期，避免两套分页 API 长期并存导致业务写法分裂。

## ADDED Requirements

### Requirement: PageParam 为唯一分页入参契约

使用框架分页能力时，分页请求参数 SHALL 以 `PageParam` 为统一入参：`pageNum` 非空且 ≥1，`pageSize` 非空且位于 1–1000（上限与系统常量一致）；经 `@Valid` 校验后转换 MyBatis-Plus 分页对象。

#### Scenario: 非法分页入参被拒绝

- **WHEN** 请求体 `pageNum=0` 或 `pageSize=0` 或 `pageSize=1001` 绑定到 `PageParam` 并启用校验
- **THEN** 参数校验失败并返回对应 400 语义（框架参数校验处理），不使用非法值执行查询

#### Scenario: 合法入参转换分页对象

- **WHEN** `pageNum=2, pageSize=20` 的 `PageParam` 调用 `toPage()`
- **THEN** 得到 current=2、size=20 的 MyBatis-Plus Page

### Requirement: PageUtils 弃用声明与保留周期

系统 SHALL 将 `PageUtils` 标记为 `@Deprecated`（类与公开方法），并通过 Javadoc 声明替代 API 与删除排期：自当前版本起保留 2 个大版本后删除；删除前其行为（页码修正、offset、总页数计算）SHALL 保持不变，不得边废弃边改语义。

#### Scenario: 弃用标记存在且行为不变

- **WHEN** 编译引用 `PageUtils.of(...)` 的既有代码并运行
- **THEN** 仅出现 deprecation 编译告警，运行结果与标记前一致（offset/calcTotalPages/非法值修正不变）

#### Scenario: 删除排期在文档可查

- **WHEN** 查看 `PageUtils` 的 Javadoc 或框架发布文档
- **THEN** 可读到：替代 API 指向（`PageParam`/`PageData`）、弃用起始版本与计划删除的大版本节点

### Requirement: 返回构造统一

分页查询结果的组装 SHALL 以 `PageData` 为准：由 MyBatis-Plus `IPage` 经 `PageData.of(IPage)` 构建，包含 list/total/pageNum/pageSize/totalPage/hasNext 语义。

#### Scenario: IPage 统一构建返回

- **WHEN** 对含 total=101、records 21 条、current=2、size=20 的 IPage 调用 `PageData.of`
- **THEN** totalPage=6、hasNext=true（末页判断正确）
