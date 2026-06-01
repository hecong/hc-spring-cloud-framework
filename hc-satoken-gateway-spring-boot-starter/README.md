# hc-satoken-gateway-spring-boot-starter

## 模块简介

`hc-satoken-gateway-spring-boot-starter` 是基于 [Sa-Token](https://sa-token.cc/) 和 Spring Cloud Gateway 的网关鉴权 Starter，为微服务架构提供统一的网关层认证授权解决方案。

本模块采用响应式编程模型（Reactor/WebFlux），专为 Spring Cloud Gateway 设计，提供了网关鉴权、配置动态刷新、统一异常处理等特性，支持 Nacos、Apollo 等配置中心。

## 设计思路

### 核心设计理念

1. **网关统一鉴权**：在网关层统一处理认证授权，避免每个微服务重复实现鉴权逻辑
2. **响应式编程**：基于 Reactor/WebFlux，适配 Spring Cloud Gateway 的响应式架构
3. **配置驱动**：鉴权规则通过配置文件管理，支持配置中心动态刷新
4. **动态路由扩展**：通过 `SaGatewayDynamicRouteProvider` SPI 支持从远程服务获取动态鉴权规则
5. **统一响应格式**：异常处理返回统一的 Result 格式，与框架其他模块保持一致

### 架构设计

```
hc-satoken-gateway-spring-boot-starter
├── config/                                # 自动配置
│   ├── SaTokenGatewayAutoConfiguration       # 核心自动配置（入口）
│   └── SaTokenGatewayRefreshListener         # 配置刷新监听器
├── filter/                                # 过滤器
│   └── SaTokenGatewayFilter                  # 网关鉴权过滤器（核心）
├── handler/                               # 处理器
│   ├── SaGatewayDynamicRouteProvider         # 动态路由权限提供者 SPI 接口
│   ├── SaTokenGatewayExceptionHandler        # 全局异常处理器
│   └── SaTokenGatewayErrorBuilder            # 错误响应构建器（静态工具）
└── properties/                            # 配置属性
    └── SaTokenGatewayProperties              # 网关配置属性（hc.satoken.gateway.*）
```

### 核心流程

#### 网关鉴权流程

```
客户端请求
    ↓
Spring Cloud Gateway 接收
    ↓
SaReactorFilter 拦截
    ↓
检查是否在排除路径（exclude-paths）
    ↓
登录校验（StpUtil.checkLogin）— 对所有非排除路径强制执行
    ↓
匹配鉴权路由规则：
  1. 优先匹配动态路由（SaGatewayDynamicRouteProvider）
  2. 回退匹配配置路由（auth-routes）
    ↓
角色校验（StpUtil.hasRole）/ 权限校验（StpUtil.hasPermission）
    ↓
转发请求到下游服务
```

#### 配置动态刷新流程

```
配置中心（Nacos/Apollo）配置变更
    ↓
Spring Cloud Context 发布 RefreshScopeRefreshedEvent
    ↓
SaTokenGatewayRefreshListener 监听并记录新配置
    ↓
@RefreshScope 自动重建 SaTokenGatewayFilter 和 SaReactorFilter
    ↓
新的鉴权规则生效
```

## 功能特性

### 1. 网关统一鉴权

- **登录校验**：对所有非排除路径强制校验登录状态
- **角色校验**：检查用户是否拥有指定角色（支持多角色，满足其一即可）
- **权限校验**：检查用户是否拥有指定权限（支持多权限，满足其一即可）
- **Ant 路径匹配**：支持 Ant 风格的路径匹配规则（如 `/api/**`）

### 2. 配置动态刷新

- **Nacos 支持**：支持 Nacos 配置中心动态刷新
- **Apollo 支持**：支持 Apollo 配置中心动态刷新
- **@RefreshScope**：鉴权过滤器和规则均支持热刷新，无需重启

### 3. 动态路由权限

- **SPI 接口**：通过 `SaGatewayDynamicRouteProvider` 接口从远程服务加载动态鉴权规则
- **优先级**：动态路由规则优先级高于配置文件中的静态规则
- **高性能匹配**：内置三层索引（精确路径 HashMap → 前缀分组 → 通配符回退列表）

### 4. 灵活的鉴权规则

- **路径级别鉴权**：为不同路径配置不同的鉴权规则
- **多角色支持**：多个角色逗号分隔，满足其一即可访问
- **多权限支持**：多个权限逗号分隔，满足其一即可访问
- **排除路径**：支持配置不需要鉴权的路径（如登录接口、公开 API）
- **匹配模式**：支持 ANY（满足任一）和 ALL（全部满足）两种模式

### 5. 统一异常处理

- **全局异常处理器**：`SaTokenGatewayExceptionHandler`（`@Order(-1)`）统一处理 Sa-Token 异常
- **标准响应格式**：通过 `SaTokenGatewayErrorBuilder` 返回与 hc-web 一致的 Result 格式（含 code、message、data、timestamp、path）
- **详细错误信息**：区分未登录、Token 无效、Token 过期、被踢下线、权限不足等多种场景

### 6. 响应式架构

- **Reactor 支持**：基于 Reactor 响应式编程
- **非阻塞 IO**：适配 Spring Cloud Gateway 的非阻塞架构
- **SaReactorFilter**：使用 Sa-Token 为 Gateway 专门设计的响应式过滤器

## 集成使用方法

### 1. 添加依赖

在网关项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-satoken-gateway-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加基础配置：

```yaml
hc:
  satoken:
    gateway:
      enabled: true                    # 是否启用网关鉴权
      exclude-paths:                   # 排除路径（不需要鉴权的路径）
        - /api/auth/login
        - /api/auth/register
        - /api/public/**
        - /actuator/**
        - /swagger-ui/**
        - /v3/api-docs/**
      auth-routes:                     # 鉴权路由规则
        - path: /api/user/**
        - path: /api/admin/**
          require-role: admin
        - path: /api/order/**
          require-permission: order:view
```

### 3. 配置 Sa-Token 基础配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

sa-token:
  token-name: Authorization
  timeout: 86400
  is-concurrent: true
  token-style: uuid
```

### 4. 实现动态路由权限提供者

实现 `SaGatewayDynamicRouteProvider` 接口，从远程服务获取动态鉴权规则：

```java
@Component
public class DynamicRouteProviderImpl implements SaGatewayDynamicRouteProvider {

    @Override
    public List<DynamicAuthRoute> loadRoutes() {
        // 从远程服务或数据库加载动态鉴权规则
        List<DynamicAuthRoute> routes = new ArrayList<>();
        routes.add(new DynamicAuthRoute("/api/dynamic/**", true, "vip", "premium:access"));
        return routes;
    }
}
```

### 5. 网关路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
```

### 6. 下游服务登录接口

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        StpUtil.login(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("userId", user.getId());
        return Result.success(data);
    }
}
```

## 配置说明

### 完整配置示例

```yaml
hc:
  satoken:
    gateway:
      enabled: true
      exclude-paths:
        - /api/auth/login
        - /api/auth/register
        - /api/public/**
        - /actuator/**
        - /swagger-ui/**
        - /v3/api-docs/**
      auth-routes:
        - path: /api/user/**
        - path: /api/admin/**
          require-role: admin
        - path: /api/order/**
          require-permission: order:view
        - path: /api/product/**
          require-permission: product:view,product:edit
        - path: /api/report/**
          require-role: admin,manager
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `hc.satoken.gateway.enabled` | 是否启用网关鉴权 | `true` |
| `hc.satoken.gateway.exclude-paths` | 排除路径列表（不需要鉴权） | 空 |
| `hc.satoken.gateway.auth-routes` | 鉴权路由规则列表 | 空 |
| `hc.satoken.gateway.auth-routes[].path` | 路径模式（Ant 风格） | - |
| `hc.satoken.gateway.auth-routes[].require-role` | 需要的角色（多个用逗号分隔） | - |
| `hc.satoken.gateway.auth-routes[].require-permission` | 需要的权限（多个用逗号分隔） | - |

> **注意**：登录校验对所有非 `exclude-paths` 中的路径强制执行，不可按路由单独配置。

## 常见问题

### 1. 网关鉴权与下游服务鉴权冲突怎么办？

建议在网关层统一处理鉴权，下游服务可移除鉴权逻辑，避免重复校验。

### 2. 如何实现网关层的动态鉴权规则？

实现 `SaGatewayDynamicRouteProvider` 接口并注册为 Spring Bean：

```java
@Component
public class MyDynamicRouteProvider implements SaGatewayDynamicRouteProvider {
    @Override
    public List<DynamicAuthRoute> loadRoutes() {
        // 从数据库或远程服务加载
        return dynamicRouteService.loadAll();
    }
}
```

### 3. 如何实现网关层的 IP 黑名单？

可以在 `SaTokenGatewayFilter.createFilter()` 中使用 `setBeforeAuth` 钩子实现，或编写自定义 `GlobalFilter`。

### 4. 如何处理跨域问题？

在网关配置中添加全局跨域配置：

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "GET,POST,PUT,DELETE,OPTIONS"
            allowedHeaders: "*"
            allowCredentials: true
```

### 5. 如何为 Gateway 提供权限数据？

在网关模块中注册一个 `StpInterface` 实现（需兼容 WebFlux 响应式环境）：

```java
@Component
public class GatewayStpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 从 Redis 或远程服务获取用户权限
        return permissionService.getPermissionsByUserId((Long) loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleService.getRolesByUserId((Long) loginId);
    }
}
```

> **注意**：`StpInterface` 需要由业务项目在网关模块中自行实现，本模块不提供默认实现。

## 最佳实践

### 1. 网关鉴权规则设计

- **粒度适中**：鉴权规则建议在服务级别配置，不宜过细
- **角色优先**：优先使用角色校验，权限校验用于细粒度控制
- **排除路径明确**：明确配置不需要鉴权的路径，避免拦截登录和健康检查接口

### 2. 配置中心使用

- **环境隔离**：使用不同的命名空间隔离不同环境的配置
- **版本管理**：配置变更前做好版本管理，支持快速回滚
- **变更通知**：配置变更后 `SaTokenGatewayRefreshListener` 会记录刷新日志

### 3. 性能优化

- **Redis 连接池**：配置合理的 Redis 连接池参数
- **动态路由缓存**：在 `SaGatewayDynamicRouteProvider` 实现中添加本地缓存
- **限流保护**：为网关添加限流保护，避免流量洪峰

### 4. 安全建议

- **HTTPS 传输**：网关对外暴露 HTTPS 接口
- **Token 有效期**：设置合理的 Token 有效期
- **日志记录**：关注认证失败的日志，便于安全审计

## 依赖说明

| 依赖 | 是否必需 | 用途 |
|---|---|---|
| `hc-common-spring-boot-starter` | 必需 | 统一的 Result 响应模型 |
| `hc-redis-spring-boot-starter` | 可选 | Redis Token 存储 |
| `sa-token-reactor-spring-boot3-starter` | 必需 | Sa-Token 响应式集成 |
| `spring-cloud-starter-gateway` | provided | Gateway 运行环境 |

## 技术支持

- Sa-Token 官方文档：https://sa-token.cc/
- Spring Cloud Gateway 官方文档：https://spring.io/projects/spring-cloud-gateway
