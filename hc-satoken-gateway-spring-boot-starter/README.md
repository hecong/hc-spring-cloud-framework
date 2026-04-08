# hc-satoken-gateway-spring-boot-starter

## 模块简介

`hc-satoken-gateway-spring-boot-starter` 是基于 [Sa-Token](https://sa-token.cc/) 和 Spring Cloud Gateway 的网关鉴权 Starter，为微服务架构提供统一的网关层认证授权解决方案。

本模块采用响应式编程模型（Reactor/WebFlux），专为 Spring Cloud Gateway 设计，提供了完善的网关鉴权、配置动态刷新、统一异常处理等特性，支持 Nacos、Apollo、腾讯北极星等配置中心。

## 设计思路

### 核心设计理念

1. **网关统一鉴权**：在网关层统一处理认证授权，避免每个微服务重复实现鉴权逻辑
2. **响应式编程**：基于 Reactor/WebFlux，适配 Spring Cloud Gateway 的响应式架构
3. **配置驱动**：所有鉴权规则通过配置文件管理，支持配置中心动态刷新
4. **灵活扩展**：通过接口扩展点（如 `SaGatewayPermissionProvider`）支持从远程服务获取权限数据
5. **统一响应格式**：异常处理返回统一的 Result 格式，与框架其他模块保持一致

### 架构设计

```
hc-satoken-gateway-spring-boot-starter
├── config/                                # 自动配置
│   ├── SaTokenGatewayAutoConfiguration       # 核心自动配置
│   └── SaTokenGatewayRefreshListener         # 配置刷新监听器
├── filter/                                # 过滤器
│   └── SaTokenGatewayFilter                  # 网关鉴权过滤器
├── handler/                               # 处理器
│   ├── SaGatewayPermissionProvider           # 权限数据提供者接口
│   ├── SaTokenGatewayStpInterface            # 权限加载实现
│   └── SaTokenGatewayExceptionHandler        # 全局异常处理器
├── model/                                 # 模型
│   └── Result                                # 统一响应结果
└── properties/                            # 配置属性
    └── SaTokenGatewayProperties              # 网关配置属性
```

### 核心流程

#### 1. 网关鉴权流程

```
客户端请求
    ↓
Spring Cloud Gateway 接收
    ↓
SaTokenGatewayFilter 拦截
    ↓
检查是否在排除路径
    ↓
匹配鉴权路由规则
    ↓
登录校验（StpUtil.checkLogin）
    ↓
角色校验（StpUtil.hasRole）
    ↓
权限校验（StpUtil.hasPermission）
    ↓
转发请求到下游服务
```

#### 2. 配置动态刷新流程

```
配置中心（Nacos/Apollo）配置变更
    ↓
Spring Cloud Context 发布 RefreshScopeRefreshedEvent
    ↓
SaTokenGatewayRefreshListener 监听事件
    ↓
更新 SaTokenGatewayProperties
    ↓
重新创建 SaReactorFilter Bean
    ↓
新的鉴权规则生效
```

#### 3. 权限数据获取流程

```
网关鉴权需要权限数据
    ↓
调用 StpInterface.getPermissionList/getRoleList
    ↓
SaTokenGatewayStpInterface 处理
    ↓
调用 SaGatewayPermissionProvider 接口
    ↓
业务实现类从远程服务获取数据
    ↓
返回权限数据给网关
```

## 功能特性

### 1. 网关统一鉴权

- **登录校验**：检查用户是否已登录
- **角色校验**：检查用户是否拥有指定角色
- **权限校验**：检查用户是否拥有指定权限
- **Ant 路径匹配**：支持 Ant 风格的路径匹配规则（如 `/api/**`）

### 2. 配置动态刷新

- **Nacos 支持**：支持 Nacos 配置中心动态刷新
- **Apollo 支持**：支持 Apollo 配置中心动态刷新
- **北极星支持**：支持腾讯北极星配置中心动态刷新
- **手动刷新**：支持通过 `/actuator/refresh` 端点手动刷新

### 3. 灵活的鉴权规则

- **路径级别鉴权**：为不同路径配置不同的鉴权规则
- **多角色支持**：支持多个角色满足其一即可访问
- **多权限支持**：支持多个权限满足其一即可访问
- **排除路径**：支持配置不需要鉴权的路径

### 4. Token 转发

- **自动转发**：自动将 Token 转发给下游服务
- **自定义 Header**：可配置 Token 转发时使用的 Header 名称
- **开关控制**：可配置是否转发 Token

### 5. 统一异常处理

- **全局异常处理器**：统一处理 Sa-Token 相关异常
- **标准响应格式**：返回统一的 Result 格式
- **详细的错误信息**：根据异常类型返回详细的错误提示

### 6. 权限数据扩展

- **远程获取**：支持从远程服务获取权限数据
- **接口扩展**：通过 `SaGatewayPermissionProvider` 接口自定义权限数据源
- **降级处理**：权限获取失败时返回空列表，不影响网关运行

### 7. 响应式架构

- **Reactor 支持**：基于 Reactor 响应式编程
- **非阻塞 IO**：适配 Spring Cloud Gateway 的非阻塞架构
- **高性能**：响应式编程模型，支持高并发场景

## 集成使用方法

### 1. 添加依赖

在网关项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.market</groupId>
    <artifactId>hc-satoken-gateway-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 或配置中心添加基础配置：

```yaml
hc:
  satoken:
    gateway:
      enabled: true                    # 是否启用网关鉴权
      forward-token: true              # 是否转发 Token 给下游服务
      forward-header-name: Authorization  # Token 转发时使用的 Header 名称
      exclude-paths:                   # 排除路径（不需要鉴权的路径）
        - /api/auth/login
        - /api/auth/register
        - /api/public/**
        - /actuator/**
        - /swagger-ui/**
        - /v3/api-docs/**
      auth-routes:                     # 鉴权路由规则
        - path: /api/user/**
          require-login: true
        - path: /api/admin/**
          require-login: true
          require-role: admin
        - path: /api/order/**
          require-login: true
          require-permission: order:view
      error-response:                  # 错误响应配置
        not-login-code: 401
        not-login-message: "请先登录"
        no-permission-code: 403
        no-permission-message: "无访问权限"
```

### 3. 配置 Sa-Token 基础配置

在 `application.yml` 中添加 Sa-Token 基础配置：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
```

### 4. 实现权限数据提供者

创建 `SaGatewayPermissionProvider` 实现类，从远程服务获取权限数据：

```java
@Component
@Slf4j
public class GatewayPermissionProviderImpl implements SaGatewayPermissionProvider {

    @Autowired
    private UserServiceClient userServiceClient;

    @Override
    public List<String> getRoles(Object loginId) {
        try {
            Result<List<String>> result = userServiceClient.getRolesByUserId(loginId);
            if (result.isSuccess()) {
                return result.getData();
            }
            log.warn("获取用户角色失败: userId={}, message={}", loginId, result.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("调用用户服务获取角色失败: userId={}", loginId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getPermissions(Object loginId) {
        try {
            Result<List<String>> result = userServiceClient.getPermissionsByUserId(loginId);
            if (result.isSuccess()) {
                return result.getData();
            }
            log.warn("获取用户权限失败: userId={}, message={}", loginId, result.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("调用用户服务获取权限失败: userId={}", loginId, e);
            return Collections.emptyList();
        }
    }
}
```

### 5. 用户服务客户端

创建用户服务的 Feign 客户端：

```java
@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/roles/{userId}")
    Result<List<String>> getRolesByUserId(@PathVariable("userId") Object userId);

    @GetMapping("/permissions/{userId}")
    Result<List<String>> getPermissionsByUserId(@PathVariable("userId") Object userId);
}
```

### 6. 配置中心集成

#### Nacos 配置

在 `bootstrap.yml` 中配置 Nacos：

```yaml
spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        refresh-enabled: true  # 启用自动刷新
        namespace: your-namespace-id
        group: DEFAULT_GROUP
      discovery:
        server-addr: localhost:8848
        namespace: your-namespace-id
```

在 Nacos 配置中心添加配置：

```yaml
hc:
  satoken:
    gateway:
      enabled: true
      auth-routes:
        - path: /api/user/**
          require-login: true
        - path: /api/admin/**
          require-login: true
          require-role: admin
      exclude-paths:
        - /api/auth/login
        - /api/public/**
```

#### Apollo 配置

在 `bootstrap.yml` 中配置 Apollo：

```yaml
app:
  id: gateway-service

apollo:
  meta: http://localhost:8080
  bootstrap:
    enabled: true
    namespaces: application
    eagerLoad:
      enabled: true  # 提前加载配置
```

#### 腾讯北极星配置

在 `bootstrap.yml` 中配置北极星：

```yaml
spring:
  application:
    name: gateway-service
  cloud:
    polaris:
      address: grpc://localhost:8091
      config:
        auto-refresh: true  # 启用自动刷新
```

### 7. 网关路由配置

在 `application.yml` 中配置网关路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=1
```

### 8. 登录接口

在用户服务中创建登录接口：

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        
        // 登录
        StpUtil.login(user.getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
```

### 9. 权限数据接口

在用户服务中创建权限数据接口：

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRoleService userRoleService;

    @GetMapping("/roles/{userId}")
    public Result<List<String>> getRolesByUserId(@PathVariable Long userId) {
        List<String> roles = userRoleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @GetMapping("/permissions/{userId}")
    public Result<List<String>> getPermissionsByUserId(@PathVariable Long userId) {
        List<String> permissions = userRoleService.getPermissionsByUserId(userId);
        return Result.success(permissions);
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
      forward-token: true
      forward-header-name: Authorization
      exclude-paths:
        - /api/auth/login
        - /api/auth/register
        - /api/auth/logout
        - /api/public/**
        - /actuator/**
        - /swagger-ui/**
        - /swagger-resources/**
        - /v3/api-docs/**
        - /webjars/**
        - /doc.html
        - /favicon.ico
      auth-routes:
        - path: /api/user/**
          require-login: true
        - path: /api/admin/**
          require-login: true
          require-role: admin
        - path: /api/order/**
          require-login: true
          require-permission: order:view
        - path: /api/product/**
          require-login: true
          require-permission: product:view,product:edit
        - path: /api/report/**
          require-login: true
          require-role: admin,manager
      error-response:
        not-login-code: 401
        not-login-message: "请先登录"
        no-permission-code: 403
        no-permission-message: "无访问权限"
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `hc.satoken.gateway.enabled` | 是否启用网关鉴权 | `true` |
| `hc.satoken.gateway.forward-token` | 是否转发 Token 给下游服务 | `true` |
| `hc.satoken.gateway.forward-header-name` | Token 转发时使用的 Header 名称 | `Authorization` |
| `hc.satoken.gateway.exclude-paths` | 排除路径列表（不需要鉴权） | 空 |
| `hc.satoken.gateway.auth-routes` | 鉴权路由规则列表 | 空 |
| `hc.satoken.gateway.auth-routes[].path` | 路径模式（Ant 风格） | - |
| `hc.satoken.gateway.auth-routes[].require-login` | 是否需要登录 | `true` |
| `hc.satoken.gateway.auth-routes[].require-role` | 需要的角色（多个用逗号分隔） | - |
| `hc.satoken.gateway.auth-routes[].require-permission` | 需要的权限（多个用逗号分隔） | - |
| `hc.satoken.gateway.error-response.not-login-code` | 未登录状态码 | `401` |
| `hc.satoken.gateway.error-response.not-login-message` | 未登录提示消息 | `"请先登录"` |
| `hc.satoken.gateway.error-response.no-permission-code` | 无权限状态码 | `403` |
| `hc.satoken.gateway.error-response.no-permission-message` | 无权限提示消息 | `"无访问权限"` |

## 高级用法

### 1. 动态添加鉴权规则

通过配置中心动态添加鉴权规则，无需重启网关：

```yaml
# 在 Nacos/Apollo 中添加新的鉴权规则
hc:
  satoken:
    gateway:
      auth-routes:
        - path: /api/new/**
          require-login: true
          require-permission: new:feature
```

### 2. 多角色/多权限校验

支持多个角色或权限满足其一即可访问：

```yaml
hc:
  satoken:
    gateway:
      auth-routes:
        - path: /api/management/**
          require-login: true
          require-role: admin,manager  # 满足其一即可
        - path: /api/data/**
          require-login: true
          require-permission: data:read,data:write  # 满足其一即可
```

### 3. 自定义权限数据缓存

在 `SaGatewayPermissionProvider` 实现中添加缓存：

```java
@Component
public class CachedGatewayPermissionProviderImpl implements SaGatewayPermissionProvider {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> getRoles(Object loginId) {
        String cacheKey = "gateway:roles:" + loginId;
        
        // 先从缓存获取
        List<String> roles = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (roles != null) {
            return roles;
        }
        
        // 从远程服务获取
        Result<List<String>> result = userServiceClient.getRolesByUserId(loginId);
        if (result.isSuccess()) {
            roles = result.getData();
            // 缓存 5 分钟
            redisTemplate.opsForValue().set(cacheKey, roles, 5, TimeUnit.MINUTES);
            return roles;
        }
        
        return Collections.emptyList();
    }

    @Override
    public List<String> getPermissions(Object loginId) {
        String cacheKey = "gateway:permissions:" + loginId;
        
        List<String> permissions = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (permissions != null) {
            return permissions;
        }
        
        Result<List<String>> result = userServiceClient.getPermissionsByUserId(loginId);
        if (result.isSuccess()) {
            permissions = result.getData();
            redisTemplate.opsForValue().set(cacheKey, permissions, 5, TimeUnit.MINUTES);
            return permissions;
        }
        
        return Collections.emptyList();
    }
}
```

### 4. 网关层 Token 验证增强

在网关层添加额外的 Token 验证逻辑：

```java
@Component
public class EnhancedSaTokenGatewayFilter extends SaTokenGatewayFilter {

    public EnhancedSaTokenGatewayFilter(SaTokenGatewayProperties properties) {
        super(properties);
    }

    @Override
    public SaReactorFilter createFilter() {
        SaReactorFilter filter = super.createFilter();
        
        // 添加前置处理
        filter.setBeforeAuth(obj -> {
            // 检查 Token 是否在黑名单中
            String token = StpUtil.getTokenValue();
            if (isTokenBlacklisted(token)) {
                throw new NotLoginException("Token 已失效", NotLoginException.INVALID_TOKEN);
            }
        });
        
        return filter;
    }

    private boolean isTokenBlacklisted(String token) {
        // 检查 Token 黑名单逻辑
        return false;
    }
}
```

## 常见问题

### 1. 网关鉴权与下游服务鉴权冲突怎么办？

建议在网关层统一处理鉴权，下游服务可以移除鉴权逻辑，或者通过配置 `forward-token: true` 将 Token 转发给下游服务，下游服务可以选择性地进行二次鉴权。

### 2. 如何实现网关层的 IP 黑名单？

可以通过自定义 `SaTokenGatewayFilter` 实现：

```java
@Override
public SaReactorFilter createFilter() {
    SaReactorFilter filter = super.createFilter();
    
    filter.setBeforeAuth(obj -> {
        ServerWebExchange exchange = (ServerWebExchange) obj;
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        
        if (isIpBlacklisted(clientIp)) {
            throw new RuntimeException("IP 已被封禁");
        }
    });
    
    return filter;
}
```

### 3. 如何实现网关层的限流？

可以结合 Spring Cloud Gateway 的 `RequestRateLimiter` 过滤器：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"
```

### 4. 如何处理跨域问题？

在网关配置中添加跨域配置：

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

### 5. 如何实现网关层的灰度发布？

可以结合 Spring Cloud Gateway 的路由权重配置：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-v1
          uri: lb://user-service-v1
          predicates:
            - Path=/api/user/**
            - Weight=user-service, 90
        - id: user-service-v2
          uri: lb://user-service-v2
          predicates:
            - Path=/api/user/**
            - Weight=user-service, 10
```

## 最佳实践

### 1. 网关鉴权规则设计

- **粒度适中**：鉴权规则不宜过细，建议在服务级别配置
- **角色优先**：优先使用角色校验，权限校验用于细粒度控制
- **排除路径明确**：明确配置不需要鉴权的路径，避免遗漏

### 2. 权限数据获取优化

- **添加缓存**：在网关层添加权限数据缓存，减少远程调用
- **异步加载**：权限数据可以异步加载，避免阻塞请求
- **降级处理**：权限服务不可用时，返回空权限列表，避免网关不可用

### 3. 配置中心使用

- **环境隔离**：使用不同的命名空间隔离不同环境的配置
- **版本管理**：配置变更前做好版本管理，支持快速回滚
- **变更通知**：配置变更后及时通知相关人员

### 4. 性能优化

- **Redis 连接池**：配置合理的 Redis 连接池参数
- **超时设置**：为远程服务调用设置合理的超时时间
- **限流保护**：为网关添加限流保护，避免流量洪峰

### 5. 安全建议

- **HTTPS 传输**：网关对外暴露 HTTPS 接口
- **Token 有效期**：设置合理的 Token 有效期
- **日志记录**：记录鉴权失败的日志，便于安全审计

## 技术支持

- Sa-Token 官方文档：https://sa-token.cc/
- Spring Cloud Gateway 官方文档：https://spring.io/projects/spring-cloud-gateway
- 项目源码：[hc-spring-cloud-framework](https://github.com/your-repo/hc-spring-cloud-framework)
- 问题反馈：[Issues](https://github.com/your-repo/hc-spring-cloud-framework/issues)

## 版本历史

- **1.0.0**：初始版本，提供网关统一鉴权、配置动态刷新、统一异常处理等功能
