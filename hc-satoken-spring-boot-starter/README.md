# hc-satoken-spring-boot-starter

## 模块简介

`hc-satoken-spring-boot-starter` 是基于 [Sa-Token](https://sa-token.cc/) 的 Spring Boot 自动配置 Starter，为微服务项目提供开箱即用的认证授权解决方案。

本模块封装了 Sa-Token 的核心功能，提供了完善的自动配置、权限管理、JWT 支持、SSO 单点登录、密码加密等特性，并与 Spring Cloud 生态无缝集成，支持配置动态刷新。

## 设计思路

### 核心设计理念

1. **开箱即用**：引入依赖后，零配置即可使用基本的登录认证功能
2. **灵活扩展**：通过接口扩展点（如 `SaPermissionProvider`）支持业务自定义
3. **配置驱动**：所有功能通过配置文件控制，支持动态刷新（Nacos/Apollo）
4. **安全增强**：提供多种密码加密算法、Token 清理机制、权限缓存等安全特性
5. **异常统一处理**：全局异常处理器，返回标准响应格式

### 架构设计

```
hc-satoken-spring-boot-starter
├── config/                      # 自动配置
│   ├── SaTokenAutoConfiguration       # 核心自动配置
│   ├── SaTokenWebMvcConfiguration     # Web MVC 拦截器配置
│   ├── SaTokenCorsConfiguration       # 跨域配置
│   ├── SaSsoAutoConfiguration         # SSO 单点登录配置
│   ├── SaTokenMyBatisPlusAutoConfiguration  # MyBatis-Plus 集成
│   ├── SaTokenRefreshConfiguration    # 配置动态刷新
│   └── SaTokenProperties              # 配置属性类
├── handler/                     # 处理器
│   ├── SaPermissionProvider          # 权限数据提供者接口
│   ├── SaTokenStpInterfaceImpl       # 权限加载实现（支持缓存）
│   ├── SaTokenExceptionHandler       # 全局异常处理器
│   └── SaTokenAuthLogger             # 认证日志记录器
├── interceptor/                 # 拦截器
│   └── SaTokenUrlInterceptor         # URL 权限拦截器
├── service/                     # 服务
│   ├── SaJwtTokenService             # JWT Token 服务
│   └── SaPermissionCacheService      # 权限缓存服务
├── scheduler/                   # 定时任务
│   └── SaTokenCleanScheduler         # Token 过期清理
└── util/                        # 工具类
    ├── SaTokenHelper                 # Token 操作工具类
    ├── SaTokenParser                 # Token 解析器
    └── SaPasswordEncoder             # 密码加密工具类
```

### 核心流程

#### 1. 认证流程

```
用户登录请求
    ↓
SaTokenHelper.login(userId)
    ↓
StpUtil.login(userId)
    ↓
生成 Token（UUID/JWT）
    ↓
存储到 Redis（可选）
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
查询用户权限（优先缓存）
    ↓
校验角色/权限
    ↓
通过/拒绝
```

## 功能特性

### 1. 自动配置

- **零配置启动**：引入依赖后自动配置 Sa-Token，无需手动配置 Bean
- **条件装配**：根据依赖和配置自动启用相应功能（JWT、SSO、Redis 等）
- **配置属性绑定**：支持 `hc.satoken.*` 配置前缀，覆盖 Sa-Token 所有核心配置

### 2. Token 管理

- **多种 Token 风格**：支持 UUID、Simple-UUID、Random-32/64/128、雪花算法、JWT
- **Token 有效期管理**：支持固定有效期、临时有效期、自动续期
- **Token 清理**：定时清理过期 Token，释放存储空间
- **并发登录控制**：支持同一账号并发登录、共享 Token

### 3. 权限管理

- **注解鉴权**：支持 `@SaCheckLogin`、`@SaCheckRole`、`@SaCheckPermission` 等注解
- **URL 拦截**：基于配置文件的 URL 权限规则，支持 Ant 风格路径匹配
- **权限缓存**：Redis 缓存用户角色和权限，减少数据库查询
- **接口扩展**：通过 `SaPermissionProvider` 接口自定义权限数据源

### 4. JWT 支持

- **JWT Token 模式**：支持 JWT Token，无需 Redis 存储
- **多种签名算法**：支持 HS256/HS384/HS512/RS256/RS384/RS512
- **Token 刷新**：支持 JWT Token 刷新
- **Claims 扩展**：支持自定义 Claims

### 5. SSO 单点登录

- **单点登录**：支持 SSO 单点登录、单点注销
- **Ticket 校验**：支持 Ticket 校验
- **REST API 模式**：支持 REST API 模式 SSO
- **多应用集成**：支持多个应用共享登录状态

### 6. 密码加密

- **多种加密算法**：支持 BCrypt、MD5、SM3（国密）
- **自动识别算法**：根据密文自动识别加密算法
- **算法切换**：通过配置切换默认加密算法

### 7. 异常处理

- **全局异常处理器**：统一处理 Sa-Token 相关异常
- **标准响应格式**：返回统一的 JSON 响应格式
- **日志记录**：自动记录认证失败日志

### 8. 跨域支持

- **自动跨域配置**：支持前后端分离项目的跨域需求
- **Token 传递**：支持从 Header、Parameter、Cookie 读取 Token
- **优先级配置**：可配置 Token 读取优先级

### 9. 日志记录

- **认证日志**：记录登录成功/失败、权限校验失败等日志
- **日志格式**：支持简单/详细两种日志格式
- **开关控制**：可配置是否记录各类日志

### 10. 配置动态刷新

- **Spring Cloud 集成**：支持 Nacos/Apollo 配置动态刷新
- **实时生效**：修改配置后实时生效，无需重启

## 集成使用方法

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.market</groupId>
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
      prefix: Bearer               # Token 前缀
      is-concurrent: true          # 是否允许并发登录
      is-share: false              # 是否共享 Token
      is-read-header: true         # 是否从 Header 读取 Token
      is-read-cookie: false        # 是否从 Cookie 读取 Token
      is-read-body: false          # 是否从请求参数读取 Token
      is-auto-renew: true          # 是否自动续期
    cookie:
      is-enabled: false            # 是否启用 Cookie
      domain: ""                   # Cookie 作用域
      path: "/"                    # Cookie 路径
      is-secure: false             # 是否仅 HTTPS 传输
      is-http-only: true           # 是否禁止 JS 读取
      same-site: Lax               # SameSite 策略
    redis:
      enabled: true                # 是否启用 Redis 存储
      key-prefix: "hc:satoken:"    # Redis Key 前缀
    token-clean:
      enabled: true                # 是否启用 Token 清理
      cron: "0 0 3 * * ?"          # 清理任务 Cron 表达式
      batch-size: 1000             # 每次清理的批次大小
    permission:
      enabled: true                # 是否启用权限校验
      cache-enabled: true          # 是否启用权限缓存
      cache-timeout: 300           # 权限缓存过期时间（秒）
    password:
      algorithm: BCRYPT            # 密码加密算法：BCRYPT / MD5 / SM3
      enabled: true                # 是否启用加密
    frontend:
      cors-enabled: true           # 是否启用跨域配置
      token-read-order: "HEADER,PARAMETER,COOKIE"  # Token 读取优先级
      cors-allowed-origins: "*"    # 跨域允许的来源
      cors-allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"  # 跨域允许的方法
      cors-allowed-headers: "*"    # 跨域允许的头
      cors-max-age: 3600           # 跨域预检请求缓存时间（秒）
      cors-allow-credentials: true # 是否允许携带凭证
    auth-log:
      login-log-enabled: true      # 是否启用登录日志
      auth-log-enabled: true       # 是否启用鉴权日志
      log-login-success: true      # 是否记录登录成功日志
      log-login-failure: true      # 是否记录登录失败日志
      log-permission-denied: true  # 是否记录权限校验失败日志
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

### 4. 登录接口

创建登录接口，使用 `SaTokenHelper` 进行登录：

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SaTokenHelper saTokenHelper;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // 1. 验证用户名密码
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 2. 验证密码
        if (!SaPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 3. 登录
        saTokenHelper.login(user.getId());

        // 4. 返回 Token
        Map<String, Object> data = new HashMap<>();
        data.put("token", saTokenHelper.getTokenValue());
        data.put("tokenName", saTokenHelper.getTokenName());
        data.put("userId", user.getId());
        data.put("username", user.getUsername());

        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        saTokenHelper.logout();
        return Result.success();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        if (!saTokenHelper.isLogin()) {
            return Result.error(401, "未登录");
        }

        Long userId = saTokenHelper.getCurrentUserId();
        User user = userService.getById(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("roles", StpUtil.getRoleList());
        data.put("permissions", StpUtil.getPermissionList());

        return Result.success(data);
    }
}
```

### 5. 权限校验

#### 方式一：注解鉴权

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/list")
    @SaCheckPermission("user:list")
    public Result<List<User>> list() {
        return Result.success(userService.list());
    }

    @PostMapping("/add")
    @SaCheckPermission("user:add")
    public Result<Void> add(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
```

#### 方式二：URL 权限规则

在 `application.yml` 中配置 URL 权限规则：

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

### 6. JWT 模式

#### 配置 JWT

```yaml
hc:
  satoken:
    token:
      style: jwt
    jwt:
      enabled: true
      secret: your-jwt-secret-key-at-least-32-characters
      timeout: 86400
      algorithm: HS256
      issuer: hc-framework
      audience: hc-app
```

#### 使用 JWT

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SaJwtTokenService jwtService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());

        // 登录后自动生成 JWT Token
        saTokenHelper.login(user.getId());
        String token = saTokenHelper.getTokenValue();

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());

        return Result.success(data);
    }

    @GetMapping("/validate")
    public Result<Boolean> validate(@RequestParam String token) {
        boolean valid = jwtService.validateToken(token);
        return Result.success(valid);
    }
}
```

### 7. SSO 单点登录

#### 添加 SSO 依赖

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-sso</artifactId>
    <version>${satoken.version}</version>
</dependency>
```

#### 配置 SSO

```yaml
hc:
  satoken:
    sso:
      enabled: true
      server-url: http://sso.example.com
      client-url: http://app.example.com
      login-path: /sso/login
      logout-path: /sso/logout
      ticket-timeout: 300
      allow-url-params: true
      secret: your-sso-secret
```

#### SSO 控制器

```java
@RestController
@RequestMapping("/sso")
public class SsoController {

    @Autowired
    private SsoConfigHolder ssoConfigHolder;

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String loginUrl = ssoConfigHolder.getFullLoginUrl("http://app.example.com/home");
        response.sendRedirect(loginUrl);
    }

    @GetMapping("/logout")
    public void logout(HttpServletResponse response) throws IOException {
        StpUtil.logout();
        String logoutUrl = ssoConfigHolder.getFullLogoutUrl("http://app.example.com");
        response.sendRedirect(logoutUrl);
    }
}
```

### 8. 密码加密

```java
@Service
public class UserServiceImpl implements UserService {

    @Override
    public void register(User user) {
        // 加密密码
        String encodedPassword = SaPasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userMapper.insert(user);
    }

    @Override
    public User login(String username, String rawPassword) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码（自动识别加密算法）
        if (!SaPasswordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        
        // 验证旧密码
        if (!SaPasswordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 加密新密码
        String encodedPassword = SaPasswordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userMapper.updateById(user);
    }
}
```

## 配置说明

### 完整配置示例

```yaml
hc:
  satoken:
    enabled: true
    
    token:
      name: Authorization
      timeout: 86400
      activity-timeout: -1
      style: uuid
      prefix: Bearer
      is-concurrent: true
      is-share: false
      is-read-header: true
      is-read-cookie: false
      is-read-body: false
      is-auto-renew: true
    
    cookie:
      is-enabled: false
      domain: ""
      path: "/"
      is-secure: false
      is-http-only: true
      same-site: Lax
    
    redis:
      enabled: true
      key-prefix: "hc:satoken:"
    
    token-clean:
      enabled: true
      cron: "0 0 3 * * ?"
      batch-size: 1000
    
    jwt:
      enabled: false
      secret: your-jwt-secret-key-at-least-32-characters
      timeout: 86400
      algorithm: HS256
      issuer: hc-framework
      audience: hc-app
    
    sso:
      enabled: false
      server-url: http://sso.example.com
      client-url: http://app.example.com
      login-path: /sso/login
      logout-path: /sso/logout
      ticket-timeout: 300
      allow-url-params: true
      secret: your-sso-secret
    
    permission:
      enabled: true
      url-permissions:
        - path: /admin/**
          role: admin
        - path: /user/**
          permission: user:view
        - path: /public/**
          require-login: false
      cache-enabled: true
      cache-timeout: 300
    
    login:
      login-paths:
        - /login
        - /auth/login
      remember-me-timeout: 604800
    
    exclude-paths:
      - /error
      - /swagger-ui/**
      - /swagger-resources/**
      - /v3/api-docs/**
      - /webjars/**
      - /doc.html
      - /favicon.ico
      - /login
      - /register
      - /public/**
    
    password:
      algorithm: BCRYPT
      enabled: true
    
    frontend:
      cors-enabled: true
      token-read-order: "HEADER,PARAMETER,COOKIE"
      cors-token-header: true
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
      log-format: SIMPLE
    
    refresh-enabled: true
    is-log: false
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `hc.satoken.enabled` | 是否启用 Sa-Token | `true` |
| `hc.satoken.token.name` | Token 名称 | `Authorization` |
| `hc.satoken.token.timeout` | Token 有效期（秒） | `86400`（30天） |
| `hc.satoken.token.style` | Token 风格 | `uuid` |
| `hc.satoken.token.prefix` | Token 前缀 | 空 |
| `hc.satoken.jwt.enabled` | 是否启用 JWT | `false` |
| `hc.satoken.jwt.secret` | JWT 密钥 | `hc-satoken-default-secret-key` |
| `hc.satoken.sso.enabled` | 是否启用 SSO | `false` |
| `hc.satoken.permission.enabled` | 是否启用权限校验 | `true` |
| `hc.satoken.permission.cache-enabled` | 是否启用权限缓存 | `true` |
| `hc.satoken.password.algorithm` | 密码加密算法 | `BCRYPT` |

## 常见问题

### 1. 如何自定义 Token 生成规则？

默认使用 UUID Token，如果需要使用 JWT Token，只需配置：

```yaml
hc:
  satoken:
    token:
      style: jwt
    jwt:
      enabled: true
      secret: your-jwt-secret
```

### 2. 如何实现多端登录控制？

通过设备标识实现多端登录控制：

```java
// PC 端登录
saTokenHelper.login(userId, "PC");

// 移动端登录
saTokenHelper.login(userId, "MOBILE");

// 踢下线指定设备
saTokenHelper.kickout(userId, "PC");
```

### 3. 如何实现记住我功能？

```java
// 记住我登录
saTokenHelper.login(userId, true);

// 配置记住我时长
hc:
  satoken:
    login:
      remember-me-timeout: 604800  # 7天
```

### 4. 如何实现权限缓存自动刷新？

权限缓存会在用户登录、注销、权限变更时自动清除。如果需要手动清除：

```java
@Autowired
private SaPermissionCacheService cacheService;

// 清除指定用户的权限缓存
cacheService.clearUserPermissionCache(userId);
```

### 5. 如何集成 Spring Security？

本模块已经提供了完整的认证授权功能，通常不需要集成 Spring Security。如果必须集成，建议将 Spring Security 作为 OAuth2 资源服务器，Sa-Token 作为内部服务的认证方案。

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
      secret: ${JWT_SECRET}   # 使用环境变量
      algorithm: HS512        # 更安全的算法
    password:
      algorithm: BCRYPT       # 推荐使用 BCrypt
    token-clean:
      enabled: true
      cron: "0 0 3 * * ?"     # 凌晨 3 点清理
```

### 2. 权限设计建议

- 使用 RBAC 模型：用户-角色-权限
- 权限标识采用 `模块:操作` 格式，如 `user:add`、`user:delete`
- 角色标识采用大写，如 `ADMIN`、`USER`
- 敏感操作使用二级认证

### 3. 安全建议

- 使用 HTTPS 传输
- Token 有效期不宜过长
- 定期更换 JWT 密钥
- 记录认证日志
- 限制登录失败次数

## 技术支持

- Sa-Token 官方文档：https://sa-token.cc/
- 项目源码：[hc-spring-cloud-framework](https://github.com/your-repo/hc-spring-cloud-framework)
- 问题反馈：[Issues](https://github.com/your-repo/hc-spring-cloud-framework/issues)

## 版本历史

- **1.0.0**：初始版本，提供基础认证授权功能
