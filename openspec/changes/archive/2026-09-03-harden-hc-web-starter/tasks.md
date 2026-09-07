## 1. 业务异常默认错误码（error-handling）

- [x] 1.1 `BusinessException` 单参构造委托为默认 400（等价 `HttpStatus.BAD_REQUEST.value()`，不新增 ResultCode 常量）；两参/三参不变；验证：`BusinessExceptionTest` 断言 `new BusinessException("xx").getCode()==400`、显式 `(500,"xx")`/自定义码/带 cause 均保持
- [x] 1.2 检索框架内/示例依赖单参默认 500 语义的用例并同步更新；验证：仓库内 `new BusinessException(` 仅两参/三参显式调用点，单参默认 500 无使用方需同步；README 注释由 code=500 更正为 code=400，`mvn -pl hc-web-spring-boot-starter test` 全绿

## 2. 补齐异常映射（error-handling）

- [x] 2.1 `GlobalExceptionHandler` 新增 `HttpRequestMethodNotSupportedException` → 405 handler（Result 结构 + path，log.warn，固定消息）；验证：MockMvc 以 DELETE 请求 GET 接口返回 405 + code/path 统一结构
- [x] 2.2 新增 `HttpMediaTypeNotSupportedException` → 415 handler；验证：MockMvc 以 text/plain 请求 application/json 接口返回 415 + 统一结构
- [x] 2.3 新增 `MaxUploadSizeExceededException` → 413 handler（固定文案，不泄漏阈值）；验证：MockMvc 抛超限异常返回 413 + 固定提示且响应不含阈值数字
- [x] 2.4 新增 `MissingPathVariableException` → 400 handler（消息含变量名）；验证：MockMvc 请求模板变量与 `@PathVariable` 名不符返回 400 + 提示含变量名

## 3. wrapper 缓存上限（request-body-caching）

- [x] 3.1 `WebProperties` 新增 `maxCachedBodySize`（long，默认 2MB）；`CustomizeRequestWrapper` 增加 `maxCachedBody` 构造参数：`contentLength>limit` 直接 `cachedBody=null`，读入后超限二次防护丢弃；`getInputStream` 在 `cachedBody==null` 时返回原始请求流；验证：`CustomizeRequestWrapperTest`——1MB 可重复读取、10MB 已知长度构造不持有超限内容（不 OOM）、800KB 自定义 100KB 上限不缓存、Content-Length 未知 3MB 二次防护丢弃
- [x] 3.2 搜索 `new CustomizeRequestWrapper(` 的装配点：仓库内仅 README 示例（外部使用），框架内部无构造点；保留默认构造兼容 + 新增带上限构造，README 示例补充 `getBodyBytes()==null` 降级路径；验证：编译 + 相关使用方用例通过

## 4. 可信代理 IP 解析（client-ip-resolution）

- [x] 4.1 `IpUtils` 新增静态 `trustedProxies` 持有者与 `setTrustedProxies`（List/可变参数）、`isTrusted`（精确 IP + IPv4 CIDR 位运算，非法项忽略）；验证：`IpUtilsTest` 覆盖精确匹配、`10.0.0.0/8`、`0.0.0.0/0`、`/32`、非法项忽略、IPv6 精确匹配
- [x] 4.2 新增 `getClientIp(request, trustedProxies)` 重载；旧 `getClientIp(request)` 改走持有者配置（缺省空）；算法覆盖：缺省空取 remoteAddr（伪造 XFF 无效）、回环优先 X-Real-IP → XFF 首 IP、可信链 XFF 右→左首个不可信、全可信回退 X-Real-IP → remote、不可信直连忽略转发头、`::1` 归一；验证：按 spec 场景逐条单测通过（含 XFF 中 unknown/畸形项跳过）
- [x] 4.3 `WebProperties` 新增 `trustedProxies`（List<String>）；`WebAutoConfiguration` 注册 `IpTrustedProxiesInitializer`（`InitializingBean`，`@ConditionalOnMissingBean`）将配置注入 `IpUtils`；验证：`WebTrustedProxiesContextTest` 配置注入生效（10.0.0.0/8 场景返回 8.8.8.8）、未配置保持安全默认、自定义初始化器 Bean 让位；业务依赖方（satoken logger / logging 限流）走公共 API `getClientIp(request)` 兼容，默认语义变更已在 README 标注 Breaking

## 5. 文档与发布说明

- [x] 5.1 README 顶部「升级指引（1.0.x → 1.1.0）」：`IpUtils.getClientIp` 缺省不再采信代理头（真实反代业务需配 `hc.web.trusted-proxies`）；业务异常默认码 400 的影响与检索命令（`grep -rn "new BusinessException("`）；验证：README 升级章节完整
- [x] 5.2 文档标注限制：IPv6 CIDR 不做完整运算、XFF 畸形值不做深度清洗、404 不在本 change；验证：README「安全加固」限制说明存在

## 6. 全量验证

- [x] 6.1 `mvn -pl hc-common-spring-boot-starter,hc-web-spring-boot-starter -am test` 全绿（IpUtils 11 + 业务异常 2 + 异常映射 4 + wrapper 5 + 上下文注入 3 = 25）；验证：本 change 相关测试通过
- [x] 6.2 全量 `mvn verify` 无回归（11/11 模块 SUCCESS）；验证：构建通过
