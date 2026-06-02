# hc-satoken-gateway-spring-boot-starter

## 模块简介

基于 [Sa-Token](https://sa-token.cc/) 和 Spring Cloud Gateway 的网关认证 Starter。**只做 Token 认证（登录校验），不做权限鉴权**。权限控制全部下沉到微服务层通过 `@SaCheckPermission` / `@SaCheckRole` 注解实现。

## 设计思路

### 架构定位

```
Gateway（本模块）                    微服务层
┌──────────────────────┐           ┌──────────────────────────┐
│ Token 认证             │           │ @SaCheckPermission       │
│ StpUtil.checkLogin()  │           │ @SaCheckRole             │
│                      │           │ 数据级权限（AOP 拦截）     │
│ 仅校验登录态 ──────────┼──────────→│                          │
│                      │           │ StpInterface（服务层实现）  │
└──────────────────────┘           └──────────────────────────┘
```

### 为什么不在网关做权限

1. **粒度太粗**：网关只能做 URL 级别匹配，无法实现"只能看自己部门的订单"这类数据级权限
2. **缓存延迟**：Session 中缓存的权限变更不实时
3. **职责不清**：`sys_permission` 表同时管菜单、按钮、网关路由，维护混乱
4. **运维负担**：权限变更后需手动刷新 Redis 缓存

### 框架不做什么

- **不实现 `UserContext` 透传**（业务项目已有 `UserContextTransmitFilter`）
- **不实现 `StpInterface`**（在微服务层实现）
- **不检查角色/权限**

---

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-satoken-gateway-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

```yaml
sa-token:
  token-name: Authorization
  timeout: 86400

hc:
  satoken:
    gateway:
      enabled: true
      exclude-paths:
        - /api/auth/login
        - /api/auth/register
        - /api/public/**
        - /actuator/**
      forward-token: true
```

### 3. 效果

- 未登录访问 `/api/user/info` → `{"code":401,"message":"请先登录"}`
- 登录后访问 `/api/user/info` → 正常放行，由下游服务处理
- 访问 `/api/auth/login` → 直接放行，不校验登录

---

## 配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `hc.satoken.gateway.enabled` | `Boolean` | `true` | 总开关 |
| `hc.satoken.gateway.exclude-paths` | `List<String>` | 空 | 不校验登录的路径（Ant 风格） |
| `hc.satoken.gateway.forward-token` | `Boolean` | `true` | 是否将 Token 写入 Header 透传给下游 |
| `hc.satoken.gateway.forward-header-name` | `String` | `Authorization` | Token 透传 Header 名 |
| `hc.satoken.gateway.error-response.not-login-code` | `Integer` | `401` | 未登录状态码 |
| `hc.satoken.gateway.error-response.not-login-message` | `String` | `请先登录` | 未登录提示消息 |

---

## 上游登录接口（业务实现）

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        StpUtil.login(user.getId());
        // 可将用户信息缓存到 Sa-Token Session，供网关 UserContextTransmitFilter 读取
        StpUtil.getSession().set("userContext", buildUserContext(user));

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        return Result.success(data);
    }
}
```

## 下游服务权限控制（业务实现）

```java
// 1. 实现 StpInterface
@Component
public class ServiceStpInterface implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermissionsByUserId((Long) loginId);
    }
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleService.getRolesByUserId((Long) loginId);
    }
}

// 2. 使用注解鉴权
@RestController
@RequestMapping("/api/order")
public class OrderController {
    @GetMapping("/{id}")
    @SaCheckPermission("order:view")
    public Result<Order> getOrder(@PathVariable Long id) { ... }

    @DeleteMapping("/{id}")
    @SaCheckPermission("order:delete")
    public Result<Void> deleteOrder(@PathVariable Long id) { ... }
}
```

---

## 异常响应格式

未登录时返回与 hc-web 一致的 Result 结构：

```json
{
  "code": 401,
  "message": "登录凭证已过期",
  "data": null,
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user/info"
}
```

未登录的详细消息由 Sa-Token `NotLoginException.type` 决定：

| 异常类型 | 默认消息 |
|----------|---------|
| `NOT_TOKEN` | 未提供登录凭证 |
| `INVALID_TOKEN` | 登录凭证无效 |
| `TOKEN_TIMEOUT` | 登录凭证已过期 |
| `BE_REPLACED` | 账号已在其他设备登录 |
| `KICK_OUT` | 账号已被踢下线 |

---

## 依赖说明

| 依赖 | 必需 | 用途 |
|------|------|------|
| `hc-common-spring-boot-starter` | 是 | 统一 Result 模型 |
| `hc-redis-spring-boot-starter` | 否 | Redis Token 存储 |
| `sa-token-reactor-spring-boot3-starter` | 是 | Sa-Token 响应式集成 |
| `spring-cloud-starter-gateway` | provided | Gateway 运行环境 |

---

## 版本历史

- **1.0.0**：初始版本，网关只做 Token 认证，权限全部下沉服务层
