# hc-satoken-spring-boot-starter

## 模块简介

`hc-satoken-spring-boot-starter` 是基于 [Sa-Token](https://sa-token.cc/) 的 Spring Boot 自动配置 Starter，为微服务项目提供开箱即用的认证授权解决方案。

本模块封装了 Sa-Token 的核心功能，提供了完善的自动配置、权限管理、JWT 支持、SSO 单点登录、密码加密等特性，并与 Spring Cloud 生态集成，支持配置动态刷新。

## 设计思路

### 核心设计理念

1. **开箱即用**：引入依赖后，零配置即可使用基本的登录认证功能
2. **灵活扩展**：通过 SPI 接口 `SaPermissionProvider` 支持业务自定义权限数据源
3. **配置驱动**：所有功能通过配置文件控制，支持动态刷新（Nacos/Apollo）
4. **安全增强**：提供多种密码加密算法、权限缓存等安全特性
5. **异常统一处理**：全局异常处理器，返回标准 Result 响应格式

### 架构设计

```
hc-satoken-spring-boot-starter
├── config/                      # 自动配置
│   ├── SaTokenAutoConfiguration        # 核心自动配置（入口）
│   ├── SaTokenWebMvcConfiguration      # Web MVC 拦截器注册
│   ├── SaTokenCorsConfiguration        # 跨域配置
│   ├── SaSsoAutoConfiguration          # SSO 配置 Holder
│   ├── SaTokenRefreshConfiguration     # 配置动态刷新（@RefreshScope）
│   └── SaTokenProperties               # 配置属性类（hc.satoken.*）
├── handler/                     # 处理器
│   ├── SaPermissionProvider           # 权限数据提供者 SPI 接口
│   ├── SaTokenStpInterfaceImpl        # StpInterface 实现（含缓存）
│   ├── SaTokenExceptionHandler        # 全局异常处理器
│   └── SaTokenAuthLogger              # 认证日志记录器
├── interceptor/                 # 拦截器
│   └── SaTokenUrlInterceptor          # URL 权限拦截器
├── service/                     # 服务
│   ├── SaJwtTokenService              # JWT Token 服务
│   └── SaPermissionCacheService       # 权限缓存服务
├── scheduler/                   # 定时任务
│   └── SaTokenCleanScheduler          # Token 清理调度器（骨架）
└── util/                        # 工具类
    ├── SaTokenHelper                  # Token 操作工具门面
    ├── SaTokenParser                  # Token 提取解析器
    └── SaPasswordEncoder              # 密码加密工具类
```

### 核心流程

#### 1. 认证流程

```
用户登录请求
    ↓
SaTokenHelper.login(userId) 或 StpUtil.login(userId)
    ↓
生成 Token（UUID 或 JWT）
    ↓
存储到 Redis
    ↓
返回 Token 给客户端
```

#### 2. 权限校验流程

```
请求到达
    ↓
SaTokenUrlInterceptor 拦截
    ↓
检查是否在排除路径
    ↓
检查登录状态
    ↓
查询用户权限（优先从 SaPermissionCacheService 缓存获取）
    ↓
校验角色/权限（支持 ANY/ALL 匹配模式）
    ↓
通过/拒绝
```

## 功能特性

### 1. 自动配置

- **零配置启动**：引入依赖后自动配置 Sa-Token
- **条件装配**：根据依赖和配置自动启用相应功能（JWT、SSO 等）
- **配置属性绑定**：支持 `hc.satoken.*` 配置前缀

### 2. Token 管理

- **多种 Token 风格**：支持 UUID、Simple-UUID、Random-32/64/128、雪花算法、JWT
- **Token 有效期管理**：支持固定有效期、自动续期
- **并发登录控制**：支持同一账号并发登录、共享 Token

### 3. 权限管理

- **注解鉴权**：支持 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解
- **URL 拦截**：基于配置文件的 URL 权限规则，支持 Ant 风格路径匹配
- **权限缓存**：通过 `SaPermissionCacheService` 在 Redis 中缓存用户角色和权限，减少数据库查询
- **SPI 扩展**：实现 `SaPermissionProvider` 接口即可自定义权限数据源

### 4. JWT 支持

- **JWT Token 模式**：支持 JWT Token（配置 `hc.satoken.token.style=jwt`）
- **多种签名算法**：支持 HS256/HS384/HS512/RS256/RS384/RS512
- **Token 刷新**：通过 `SaJwtTokenService` 支持 JWT Token 刷新和过期检查

> **注意**：启用 JWT 模式时必须配置 `hc.satoken.jwt.secret`，否则应用启动会失败。

### 5. SSO 单点登录

- **配置支持**：通过 `SsoConfigHolder` 提供 SSO 认证中心 URL 构建能力
- **重定向白名单**：内置重定向 URL 校验，防止开放重定向攻击
- **注意**：本模块提供 SSO 配置管理和 URL 构建，实际的 SSO 登录/注销端点需要业务项目自行实现

### 6. 密码加密

- **多种加密算法**：支持 BCrypt（推荐）、MD5（带盐/不带盐）、SM3（国密算法）
- **自动识别算法**：`SaPasswordEncoder.matches()` 根据密文格式自动识别加密算法
- **算法切换**：通过 `hc.satoken.password.algorithm` 切换默认加密算法

### 7. 异常处理

- **全局异常处理器**：`SaTokenExceptionHandler` 统一处理 Sa-Token 相关异常
- **标准响应格式**：返回统一的 Result 格式（401/403/423/500）
- **认证日志**：`SaTokenAuthLogger` 自动记录认证失败日志（支持 SIMPLE/DETAIL 格式）

### 8. 跨域支持

- **自动跨域配置**：支持前后端分离项目的跨域需求
- **Token 传递**：`SaTokenParser` 支持从 Header、Parameter、Cookie 读取 Token
- **优先级配置**：可配置 Token 读取优先级顺序

### 9. 配置动态刷新

- **Spring Cloud 集成**：通过 `SaTokenRefreshConfiguration` 支持 Nacos/Apollo 配置动态刷新
- **实时生效**：修改密码算法等配置后实时生效，无需重启

## 集成使用方法

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-satoken-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加基础配置：

```yaml
hc:
  satoken:
    enabled: true
    token:
      name: Authorization          # Token 名称
      timeout: 86400               # Token 有效期（秒），默认 30 天
      style: uuid                  # Token 风格：uuid / jwt
      prefix:                      # Token 前缀（如 Bearer）
      is-concurrent: true          # 是否允许并发登录
      is-share: false              # 是否共享 Token
      is-read-header: true         # 是否从 Header 读取 Token
      is-read-cookie: false        # 是否从 Cookie 读取 Token
      is-read-body: false          # 是否从请求参数读取 Token
      is-auto-renew: true          # 是否自动续期
    redis:
      enabled: true                # 是否启用 Redis 存储
      key-prefix: "hc:satoken:"    # Redis Key 前缀
    token-clean:
      enabled: true                # 是否启用 Token 清理调度
      cron: "0 0 3 * * ?"         # 清理任务 Cron 表达式
    permission:
      enabled: true                # 是否启用权限校验
      cache-enabled: true          # 是否启用权限缓存
      cache-timeout: 300           # 权限缓存过期时间（秒）
    password:
      algorithm: BCRYPT            # 密码加密算法：BCRYPT / MD5 / SM3
    frontend:
      cors-enabled: false          # 是否启用跨域配置（建议显式开启）
      token-read-order: "HEADER,PARAMETER,COOKIE"  # Token 读取优先级
      cors-allowed-origins: "*"
      cors-allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
      cors-allowed-headers: "*"
      cors-max-age: 3600
      cors-allow-credentials: true
    auth-log:
      login-log-enabled: true
      auth-log-enabled: true
      log-login-success: true
      log-login-failure: true
      log-permission-denied: true
      log-format: SIMPLE           # 日志格式：SIMPLE / DETAIL
    refresh-enabled: true          # 是否支持配置动态刷新
    exclude-paths:                 # 排除路径（不需要鉴权的路径）
      - /login
      - /register
      - /public/**
```

### 3. 实现权限数据提供者

创建 `SaPermissionProvider` 实现类，提供用户角色和权限数据：

```java
@Service
public class MyPermissionProviderImpl implements SaPermissionProvider {

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionService permissionService;

    @Override
    public List<String> getRoles(Long userId) {
        return userRoleService.getRolesByUserId(userId);
    }

    @Override
    public List<String> getPermissions(Long userId) {
        return permissionService.getPermissionsByUserId(userId);
    }
}
```

### 4. 使用注解鉴权

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/list")
    @SaCheckPermission("user:list")
    public Result<List<User>> list() { ... }

    @PostMapping
    @SaCheckPermission("user:add")
    public Result<Void> add(@RequestBody User user) { ... }

    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable Long id) { ... }
}
```

### 5. 使用 SaTokenHelper 工具门面

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SaTokenHelper saTokenHelper;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        saTokenHelper.login(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", saTokenHelper.getTokenValue());
        data.put("userId", user.getId());
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        saTokenHelper.logout();
        return Result.success();
    }
}
```

### 6. JWT 模式

```yaml
hc:
  satoken:
    token:
      style: jwt
    jwt:
      enabled: true
      secret: your-jwt-secret-key-at-least-32-characters  # 必填！
      timeout: 86400
      algorithm: HS256
```

### 7. URL 权限规则

在 `application.yml` 中配置：

```yaml
hc:
  satoken:
    permission:
      enabled: true
      url-permissions:
        - path: /admin/**
          role: admin
        - path: /user/**
          permission: user:view
        - path: /public/**
          require-login: false
```

## 配置说明

### 配置项说明

| 配置项 | 说明 | 默认值 |
|---|---|---|
| `hc.satoken.enabled` | 是否启用 Sa-Token | `true` |
| `hc.satoken.token.name` | Token 名称 | `Authorization` |
| `hc.satoken.token.timeout` | Token 有效期（秒） | `2592000`（30天） |
| `hc.satoken.token.style` | Token 风格 | `uuid` |
| `hc.satoken.token.prefix` | Token 前缀 | 空 |
| `hc.satoken.jwt.enabled` | 是否启用 JWT | `false` |
| `hc.satoken.jwt.secret` | JWT 密钥（启用 JWT 时必填） | `null` |
| `hc.satoken.sso.enabled` | 是否启用 SSO 配置 | `false` |
| `hc.satoken.permission.enabled` | 是否启用权限校验 | `true` |
| `hc.satoken.permission.cache-enabled` | 是否启用权限缓存 | `true` |
| `hc.satoken.password.algorithm` | 密码加密算法 | `BCRYPT` |

## 常见问题

### 1. 如何自定义 Token 生成规则？

配置 Token 风格：

```yaml
hc:
  satoken:
    token:
      style: jwt      # 或 uuid / random-32 / random-64 等
    jwt:
      enabled: true
      secret: your-jwt-secret  # JWT 模式必须配置密钥
```

### 2. 如何实现多端登录控制？

通过设备标识实现：

```java
saTokenHelper.login(userId, "PC");     // PC 端登录
saTokenHelper.login(userId, "MOBILE"); // 移动端登录
saTokenHelper.kickout(userId, "PC");   // 踢下线指定设备
```

### 3. 如何自定义权限数据源？

实现 `SaPermissionProvider` 接口并注册为 Spring Bean，框架会自动注入到 `SaTokenStpInterfaceImpl` 中。

### 4. 如何手动清除用户权限缓存？

```java
@Autowired
private SaPermissionCacheService cacheService;

// 清除指定用户的权限缓存
cacheService.clearUserCache(userId);
```

## 最佳实践

### 1. 生产环境配置建议

```yaml
hc:
  satoken:
    token:
      timeout: 7200           # 2小时
      activity-timeout: 1800  # 30分钟无操作过期
      is-concurrent: false    # 禁止并发登录
      is-auto-renew: true     # 自动续期
    jwt:
      secret: ${JWT_SECRET}   # 使用环境变量，不要硬编码
      algorithm: HS512
    password:
      algorithm: BCRYPT       # 推荐使用 BCrypt
```

### 2. 权限设计建议

- 使用 RBAC 模型：用户 → 角色 → 权限
- 权限标识采用 `模块:操作` 格式，如 `user:add`、`order:view`
- 敏感操作使用二级认证

## 依赖说明

本模块依赖以下内部框架模块（必需）：

| 模块 | 用途 |
|---|---|
| `hc-common-spring-boot-starter` | 共享常量和工具类 |
| `hc-web-spring-boot-starter` | 统一 Result 响应和全局异常处理 |
| `hc-redis-spring-boot-starter` | Token 存储和权限缓存 |

## 技术支持

- Sa-Token 官方文档：https://sa-token.cc/
