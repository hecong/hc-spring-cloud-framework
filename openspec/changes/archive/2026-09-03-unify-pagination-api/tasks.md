## 1. 弃用标记（mybatis/pagination）

- [x] 1.1 `PageUtils` 类与全部公开方法标记 `@Deprecated`，类 Javadoc 增加弃用说明（替代 API：`PageParam`/`PageData`；弃用起始 1.1.0；删除排期 ≥3.0）；实例 getter 以显式 `@Deprecated` 方法补齐；类内已弃用方法互调（`toString`→`getOffset`、实例/静态 `calcTotalPages` 等）位于已弃用类内不会告警，无需 `@SuppressWarnings`（IDE/编译器均确认）；验证：`mvn -pl hc-common-spring-boot-starter compile/test` 通过
- [x] 1.2 行为保持验证：新增 `PageUtilsTest`（10 条）断言 `of(0,0)`/`of(-5,-1)` 修正为 1、每页 5000 收敛到 `MAX_PAGE_SIZE`、`getOffset`（0/10/100/修正后 2000）、`calcTotalPages(0/100/101, 10)`、负总数、实例方法、`defaultPage`、`isFirstPage`/`isLastPage` 边界与弃用前一致；验证：21/21 全绿（IpUtils 11 + PageUtils 10）

## 2. 示例迁移

- [x] 2.1 检索框架与示例中 `PageUtils` 使用点：全仓 Java 代码仅 hc-common 自身引用，无框架/示例使用点；README 分页示例（§7 统一分页、§高级用法 2 手写分页、架构图/分页流程/最佳实践）统一改为 `PageParam`（`@Valid`）+ `toPage()`，返回组装改 `PageData.of(IPage)`，并修正历史遗留错误文案（`PageResult`/`page`/`size`/`toMpPage` 等非真实 API）；验证：示例无 `PageUtils` 使用、无 deprecation 残留
- [x] 2.2 确认无 `PageUtils` 依赖的本仓库业务代码残留：`PageUtils\.|new PageUtils|import .*\.PageUtils` 检索仅命中 hc-common 自身（javadoc 与私有构造），框架无业务调用方；验证：检索结果为空（除自身与文档）

## 3. 文档

- [x] 3.1 README 发布说明新增「分页 API 收敛（自 1.1.0）」：新旧写法对照表（Controller 入参 / toPage 转换 / PageData.of 组装 / pageResult 快捷查询）、`PageUtils` 删除排期（≥3.0）、原生 SQL 用户过渡指引（分页插件即时迁移、纯手写 SQL 内联 `(pageNum-1)*pageSize`）；hc-common README 工具列表标注弃用并指向替代文档；mybatis-plus README 版本历史新增 1.1.0 条目；验证：README 章节完整、迁移路径可执行
