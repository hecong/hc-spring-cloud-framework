## Context

见 proposal.md - Why 与对应 spec。代码基线（已核对）：
- `BusinessException`：单参构造 `this.code = 500`；两参/三参显式 code。`GlobalExceptionHandler.handleBusinessException` 无 `@ResponseStatus`（HTTP 200 + body code），其余 handler 均带 `@ResponseStatus`。
- `GlobalExceptionHandler` 已覆盖：重复提交(429)、业务异常、`MethodArgumentNotValid`(400)、`Bind`(400)、`ConstraintViolation`(400)、缺参(400)、类型不匹配(400)、`IllegalArgument`(400)、`IllegalState`(500)、兜底 `Exception`(500)。**缺**：405/415/413/`MissingPathVariable`。
- `CustomizeRequestWrapper`：构造即 `ServletUtils.getBodyBytes(request)` 全量读入 `byte[]`，无上限。
- `WebProperties`（前缀 `hc.web`）：无 body 上限与可信代理配置。
- `IpUtils`（hc-common）：`getClientIp(HttpServletRequest)` 信任 XFF 首 IP；`::1` 归一 `127.0.0.1`；无 CIDR 能力。
- 依赖关系：hc-common 不依赖 hc-web（配置 `hc.web.trusted-proxies` 属于 web 层）；框架无 `ResultCode` 常量类（改进方案示例代码引用它，需等价替换）。

## Goals / Non-Goals

**Goals:**
- 三个改动集中、可单测；均不改变接口/方法签名对外形态（除行为变更）。
- 可信代理白名单能力放 hc-common（`IpUtils` 重载），配置与装配放 hc-web，低模块耦合。

**Non-Goals:**
- 不支持 IPv6 CIDR 的完整运算（IP 匹配以 IPv4 精确/CIDR 为主，IPv6 精确匹配可用；文档标注限制）。
- 不处理 `XFF` 中带端口/畸形 IP 的复杂清洗（仅做空白与 basic trim + 长度校验）。
- 不在本 change 处理 404（`NoHandlerFoundException`，需 `throw-exception-if-no-handler-found` 等容器开关，另议）。

## Decisions

### D1. P1-4 默认错误码 400

`BusinessException(String)` 改为委托 `this(HttpStatus.BAD_REQUEST.value(), message)`（与 `GlobalExceptionHandler` 既有 handler 一致使用数值 400；框架无 `ResultCode` 常量，不新增）。HTTP 状态码语义保持现状（业务异常 HTTP 200 + body code），因此 **BREAKING 面集中在 body code 500→400**，前端依赖 body.code==500 判"系统错误"的逻辑需核对。两参/三参构造不变。

验证：单测 `new BusinessException("xx").getCode()==400`；框架内若存在依赖 500 的业务异常默认语义的用例需同步更新。

### D2. P2-3 补齐异常映射

新增 4 个 handler（均 `@ExceptionHandler` + `@ResponseStatus`，返回统一 `Result.error(status, message)` + `setPath`）：
- `HttpRequestMethodNotSupportedException` → 405，消息含方法与路径提示（不拼堆栈）。
- `HttpMediaTypeNotSupportedException` → 415，固定提示"不支持的请求内容类型"。
- `MaxUploadSizeExceededException` → 413，固定提示"上传文件大小超过限制"（不泄漏内部阈值）。
- `MissingPathVariableException` → 400，消息含变量名。
日志统一 `log.warn`（客户端错误不上报 error 告警）。

优先级：Spring 按异常类型最近匹配，与既有 handler 不冲突；`Exception` 兜底不吞这些映射。注意 `MaxUploadSizeExceededException` 在某些容器可能包在 `MultipartException` 内——若验证发现需同时覆盖 `MultipartException` 或解包（tasks 验证点）。

### D3. wrapper 缓存上限

`CustomizeRequestWrapper` 改造：
- 新增构造参数 `maxCachedBody`（沿用默认 2MB 常量）；`WebProperties` 增加 `maxCachedBodySize`（long，默认 `2*1024*1024`）。
- 构造流程按改进方案：`contentLength > limit` → `cachedBody=null` 直接返回（不读流）；否则读入后若 `length > limit`（声明缺失/欺骗的二次防护）→ 置 `cachedBody=null`。
- `getInputStream()`：`cachedBody != null` 时返回缓存字节流（现逻辑）；`cachedBody == null` 时返回原始请求 `getInputStream()`，可读性行为降级为"直接消费原始流"。wrapper 使用方需感知：读不到缓存体时日志切面输出 `<body not cached>`。
- 装配点：目前 wrapper 由调用方（如日志切面/过滤器）构造，搜索实际使用 `new CustomizeRequestWrapper(` 的位置后统一传入 `webProperties.getMaxCachedBodySize()`（tasks 验证点）；若仅框架内部构造，改动集中在构造处。

风险说明：未知长度 + 实际超限的路径需要先读入才知超限，读入过程中原始流已部分消费；该路径本就无法"不读而知"，文档接受此降级（超大请求不被缓存、不 OOM），并注明缓存不可用时依赖二次读取的功能降级。

### D4. 客户端 IP 解析与配置注入

`IpUtils`（hc-common）：
- 新增静态持有者 `trustedProxies`（`volatile List<String>`，默认空）+ `setTrustedProxies(List)`/`setTrustedProxies(String[])`（供启动注入）。
- 新增 `getClientIp(HttpServletRequest, List<String>)` 重载实现解析算法；**旧 `getClientIp(HttpServletRequest)` 改走"持有者配置 + 算法"**，行为随注入的配置变化（缺省即安全默认）。
- `isTrusted(ip, trusted)`：精确匹配或 IPv4 CIDR（前缀长度 ≤32，位运算掩码比对）；无效项忽略。
- `normalizeIp`：保留 `::1 → 127.0.0.1`。

解析算法（对应 spec 三个场景）：
1. `remote = normalize(remoteAddr)`。
2. 若 `trusted` 为空：仅 `remote` 为回环时依次取 `X-Real-IP` → `XFF` 首 IP（本机代理/调试），否则返回 `remote`。
3. 若 `trusted` 非空且 `isTrusted(remote, trusted)`：取 `XFF`（存在时）右→左首个不可信 IP 返回；全可信/无 XFF 时尝试 `X-Real-IP`；再回退 `remote`。
4. 否则（不可信直连）返回 `remote`，忽略全部转发头。

装配（hc-web）：`WebProperties` 增加 `trustedProxies`（`List<String>`）；`WebAutoConfiguration` 注册一个 `ApplicationRunner`/`@PostConstruct` 初始化器，把 `webProperties.getTrustedProxies()` 注入 `IpUtils.setTrustedProxies`（`@ConditionalOnMissingBean(IpTrustedProxiesInitializer.class)` 防重复）。未引入 hc-web 依赖的服务中 `IpUtils` 保持空可信列表的安全默认。

### D5. 发布顺序与兼容

1. `IpUtils` 行为变更（缺省不采信代理头）为 **BREAKING**：真实反代后的业务必须配置 `hc.web.trusted-proxies`；README 顶部升级指引列出排查步骤（`ip` 是否为代理 IP 段、XFF 是否被丢弃）。
2. 业务异常默认码变化在 README 标注"依赖 body.code==500 的调用点需核对"。
3. wrapper 上限与 4 类异常补齐无迁移成本。

## Risks / Trade-offs

- [P1-4 默认码 500→400 误伤依赖 500 语义的前端判断] → BREAKING 标注 + 业务检索指引；提供两参构造显式 500 的等价迁移写法。
- [P1-8 缺省行为变化导致未配置反代后的业务丢失真实 IP] → README 醒目发布说明；可先用 `trusted-proxies` 灰度。
- [wrapper 超限后原始流消费副作用] → 文档说明"不缓存"语义边界；大上传接口若依赖 wrapper 二次读取需评估。
- [`MaxUploadSizeExceededException` 包装形态因容器而异] → tasks 验证并可能补充 `MultipartException` 处理。
- [IPv4 CIDR 匹配精度] → 位运算标准实现；测试覆盖边界（0.0.0.0/0、/32、/8）。

## Migration Plan

1. 升级前：识别部署拓扑（是否有反代/Nginx/LB）并准备 `hc.web.trusted-proxies` 值；检索 `new BusinessException(` 依赖 500 的调用点。
2. 升级后：先验证 `IpUtils.getClientIp` 返回值符合拓扑预期；业务异常抽样核对响应 body code 变化。
3. 回滚：还原框架版本；wrapper/IP 行为随版本回退（注意安全窗口，回滚后 IP 伪造面恢复原状需同步评估）。

## Open Questions

- 暂无。
