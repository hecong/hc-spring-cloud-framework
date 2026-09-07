# Client IP Resolution Specification

## Purpose

定义客户端真实 IP 的解析规则：仅在可信代理条件下采信代理转发头，杜绝攻击者伪造 `X-Forwarded-For`/`X-Real-IP` 绕过限流、黑名单与审计；缺省不信任任何代理并保持本机回环场景可调试。

## Requirements

### Requirement: 缺省不信任代理头

系统在未配置可信代理列表（`hc.web.trusted-proxies` 缺省为空）时，解析客户端 IP SHALL 直接采用 TCP 对端地址 `remoteAddr`（`::1` 归一为 `127.0.0.1`），不得采信任何代理转发头。

#### Scenario: 缺省下伪造 XFF 无效

- **WHEN** 未配置可信代理，请求携带伪造的 `X-Forwarded-For: 1.2.3.4` 且 `remoteAddr` 为公网地址
- **THEN** 解析结果等于 `remoteAddr`（伪造的 XFF 头被忽略）

### Requirement: 本地回环场景的 X-Real-IP 支持

系统在 `remoteAddr` 为本地回环地址时，SHALL 支持 `X-Real-IP`：当该头存在且合法时优先返回，其次尝试 `X-Forwarded-For` 首 IP，兜底回环地址（用于本机 Nginx 单跳代理/调试）。

#### Scenario: 回环地址优先取 X-Real-IP

- **WHEN** `remoteAddr` 为 `127.0.0.1`/`::1`，请求带 `X-Real-IP: 10.20.30.40` 与伪造的 `X-Forwarded-For: 1.2.3.4`
- **THEN** 解析结果为 `10.20.30.40`（本机代理透传的真实 IP 优先）

### Requirement: 可信代理链解析

系统在配置可信代理列表后，对来自可信代理的请求（`remoteAddr` ∈ 可信列表，精确 IP 或 CIDR 网段），SHALL 从 `X-Forwarded-For` 右侧向左跳过可信代理 IP，返回第一个不可信 IP；若链中全为可信 IP 或头缺失，回退可信代理列表内最右侧地址；仍不可用时回退 `remoteAddr`。

#### Scenario: 多级可信代理链解析

- **WHEN** 配置可信代理为 `10.0.0.0/8`，请求 `remoteAddr=10.0.0.5`，`X-Forwarded-For: 8.8.8.8, 10.0.0.4, 10.0.0.5`
- **THEN** 解析结果为 `8.8.8.8`（右向左首个不可信 IP）

#### Scenario: 来自不可信直连请求忽略转发头

- **WHEN** `remoteAddr` 为公网地址（不在可信列表），请求携带任意 `X-Forwarded-For`/`X-Real-IP`
- **THEN** 解析结果等于 `remoteAddr`，转发头不被采信

### Requirement: 可信列表支持精确 IP 与 CIDR

系统可信代理列表 SHALL 支持精确 IPv4 地址与 CIDR 网段（如 `10.0.0.0/8`）；非法配置项 SHALL 被忽略而不影响其余项生效。

#### Scenario: CIDR 与精确 IP 混合配置

- **WHEN** 配置 `hc.web.trusted-proxies=10.0.0.0/8,192.168.1.1`，`remoteAddr` 为 `10.1.2.3` 或 `192.168.1.1`
- **THEN** 均被识别为可信代理并执行代理链解析
