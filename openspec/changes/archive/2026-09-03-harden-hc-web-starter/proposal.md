## Why

`hc-web-spring-boot-starter` 与客户端 IP 解析存在四类问题：①`BusinessException` 单参构造器默认错误码 500——参数不合法/状态冲突等业务异常语义上属 4xx，前端无法区分"业务错误"与"系统错误"；②`CustomizeRequestWrapper` 构造时一次性把整个请求体读入内存（`byte[]`），大请求可致 OOM；③`IpUtils.getClientIp` 无条件信任 `X-Forwarded-For` 第一个 IP——未部署在可信代理后时任何人都可伪造请求头绕过限流/黑名单/审计；④`GlobalExceptionHandler` 缺方法不支持（405）、Content-Type 不支持（415）、上传超限（413）、路径变量缺失等常见客户端错误映射。

## What Changes

- **BREAKING** `BusinessException` 单参构造器默认错误码 500 → 400（语义对齐 4xx）；README 标注需全局检索业务中依赖 500 语义的调用点。
- `CustomizeRequestWrapper` 增加缓存体大小上限：超过 `hc.web.max-cached-body-size`（默认 2MB）的请求体**降级不缓存**（`cachedBody=null`，`getInputStream` 返回原始流），避免 OOM；Content-Length 缺失/被欺骗时读入后二次防护丢弃。
- `IpUtils` 增加可信代理白名单解析：新增 `getClientIp(request, trustedProxies)` 重载与启动注入通道；`hc.web.trusted-proxies`（支持精确 IP 与 CIDR）缺省为空 = **不信任任何代理，直接取 `remoteAddr`**；配置后从 `X-Forwarded-For` 右向左跳过可信代理取首个不可信 IP；`remoteAddr` 为本地回环时优先取 `X-Real-IP`（nginx 单跳场景）。
- `GlobalExceptionHandler` 补齐 4 类客户端错误映射：方法不支持（405）、Content-Type 不支持（415）、上传超限（413）、缺少路径变量（400，`MissingPathVariableException`）；响应统一 Result 结构、固定文案防细节泄漏。

## Capabilities

### New Capabilities

- `web/error-handling`：业务异常默认错误码语义（4xx）与常见 HTTP/客户端错误到统一错误响应的映射。
- `web/request-body-caching`：请求体可缓存上限与超限降级语义（防 OOM）。
- `web/client-ip-resolution`：客户端 IP 解析的可信代理白名单（含 CIDR）、伪造头防护与 X-Real-IP 支持。

### Modified Capabilities

- （无：`openspec/specs/` 尚无既有 capability）

## Impact

- **代码**：
  - `hc-web-spring-boot-starter`：`exception/BusinessException`（默认码 400）、`exception/GlobalExceptionHandler`（补 4 类 handler）、`wrapper/CustomizeRequestWrapper`（缓存上限与降级流）、`config/WebProperties`（新增 `maxCachedBodySize`、`trustedProxies`）、`config/WebAutoConfiguration`（trusted-proxies 注入通道）。
  - `hc-common-spring-boot-starter`：`util/IpUtils`（新增可信列表重载 + CIDR 匹配 + 启动注入点）。
- **配置**：新增 `hc.web.max-cached-body-size`（默认 2MB）、`hc.web.trusted-proxies`（默认空）。
- **发布影响**：P1-4 与 P1-8 为行为变更——业务异常 body code 由 500 变 400；`IpUtils.getClientIp(request)` 默认不再采信代理头，真实反代后的服务需配置 `trusted-proxies`（README 醒目标注升级指引）。
