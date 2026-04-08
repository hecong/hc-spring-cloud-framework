# hc-web-spring-boot-starter

## 模块简介

`hc-web-spring-boot-starter` 是 Spring Boot Web 应用的基础增强 Starter，为微服务项目提供统一的 Web 层解决方案。

本模块封装了 Spring Boot Web 开发的常用功能，提供了统一响应格式、全局异常处理、XSS 防护、响应自动包装等特性，简化 Web 层开发，提高开发效率。

## 设计思路

### 核心设计理念

1. **统一响应格式**：所有接口返回统一的 Result 格式，便于前端统一处理
2. **自动包装**：通过 ResponseBodyAdvice 自动包装返回值，减少重复代码
3. **全局异常处理**：统一处理各类异常，返回标准错误响应
4. **XSS 防护**：通过 Filter 和 Jackson 反序列化器双重防护，保障应用安全
5. **动态字段名**：支持自定义响应字段名，适配不同的前端规范
6. **开箱即用**：引入依赖后自动配置，零配置即可使用

### 架构设计

```
hc-web-spring-boot-starter
├── config/                      # 自动配置
│   ├── WebAutoConfiguration       # 核心自动配置
│   └── WebProperties              # 配置属性类
├── exception/                   # 异常处理
│   ├── BusinessException          # 业务异常
│   └── GlobalExceptionHandler     # 全局异常处理器
├── model/                       # 模型
│   └── Result                     # 统一响应结果
├── serializer/                  # 序列化
│   └── ResultSerializer           # Result 序列化器
├── util/                        # 工具类
│   └── ServletUtils               # Servlet 工具类
├── wrapper/                     # 包装器
│   ├── CustomizeRequestWrapper   # 请求包装器
│   └── ResponseWrapAdvice         # 响应自动包装
└── xss/                         # XSS 防护
    ├── XssFilter                  # XSS 过滤器
    ├── XssHttpServletRequestWrapper  # XSS 请求包装器
    ├── XssKit                     # XSS 工具类
    └── XssStringDeserializer      # XSS 字符串反序列化器
```

### 核心流程

#### 1. 请求处理流程

```
客户端请求
    ↓
XssFilter 过滤（XSS 防护）
    ↓
Controller 处理
    ↓
返回数据
    ↓
ResponseWrapAdvice 拦截
    ↓
自动包装为 Result
    ↓
ResultSerializer 序列化（动态字段名）
    ↓
返回 JSON 给客户端
```

#### 2. 异常处理流程

```
Controller 抛出异常
    ↓
GlobalExceptionHandler 捕获
    ↓
根据异常类型处理
    ↓
构建 Result 错误响应
    ↓
返回 JSON 给客户端
```

#### 3. XSS 防护流程

```
客户端请求
    ↓
XssFilter 拦截
    ↓
检查是否在排除路径
    ↓
包装请求对象（XssHttpServletRequestWrapper）
    ↓
Jackson 反序列化时调用 XssStringDeserializer
    ↓
XssKit.escape 过滤危险脚本
    ↓
返回安全的参数值
```

## 功能特性

### 1. 统一响应格式

- **标准 Result 结构**：包含 code、message、data、timestamp、path 字段
- **静态工厂方法**：提供 success()、error() 等便捷方法
- **泛型支持**：支持任意类型的数据
- **时间戳自动生成**：自动添加响应时间戳

### 2. 响应自动包装

- **自动包装**：通过 ResponseBodyAdvice 自动包装返回值为 Result
- **开关控制**：可通过配置关闭自动包装
- **智能判断**：自动判断返回值类型，避免重复包装
- **文本兼容**：对 String 类型特殊处理，避免破坏文本响应

### 3. 全局异常处理

- **业务异常**：统一处理 BusinessException
- **参数校验异常**：处理 @Valid/@Validated 校验失败
- **参数绑定异常**：处理参数绑定失败
- **缺少参数异常**：处理缺少必要参数
- **参数类型不匹配**：处理参数类型转换失败
- **其他异常**：兜底处理所有未捕获异常

### 4. XSS 防护

- **Filter 过滤**：通过 Servlet Filter 过滤请求参数
- **Jackson 反序列化**：在 JSON 反序列化时过滤字符串
- **智能过滤**：只移除危险脚本，保留正常 HTML 字符
- **排除路径**：支持配置不需要 XSS 过滤的路径
- **两种模式**：提供普通模式和严格模式

### 5. 动态字段名

- **自定义字段名**：支持自定义 code、message、data 字段名
- **Jackson 集成**：通过自定义序列化器实现
- **配置驱动**：通过配置文件控制字段名

### 6. 工具类

- **ServletUtils**：提供常用的 Servlet 操作方法
  - 获取请求对象
  - 获取客户端 IP
  - 获取 User-Agent
  - 返回 JSON 响应
  - 返回附件
  - 获取请求体

## 集成使用方法

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.market</groupId>
    <artifactId>hc-web-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加基础配置（可选）：

```yaml
hc:
  web:
    enabled: true                    # 是否启用 Web Starter（默认 true）
    wrap-response: true              # 是否自动包装响应（默认 true）
    code-field: code                 # 响应码字段名（默认 code）
    message-field: message           # 响应消息字段名（默认 message）
    data-field: data                 # 响应数据字段名（默认 data）
    xss-enabled: true                # 是否启用 XSS 防护（默认 true）
    xss-exclude-urls:                # XSS 排除路径
      - /api/editor/**
      - /api/rich-text/**
```

### 3. 使用统一响应格式

#### 方式一：手动返回 Result

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    @PostMapping
    public Result<Void> createUser(@RequestBody @Valid UserDTO userDTO) {
        userService.create(userDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return Result.success("删除成功");
    }
}
```

#### 方式二：自动包装（推荐）

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public void createUser(@RequestBody @Valid UserDTO userDTO) {
        userService.create(userDTO);
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        return userService.list();
    }
}
```

自动包装后的响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "张三",
    "email": "zhangsan@example.com"
  },
  "timestamp": "2026-04-07 10:30:00",
  "path": "/api/user/1"
}
```

### 4. 使用业务异常

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        
        return user;
    }

    @Override
    public void create(UserDTO userDTO) {
        // 检查用户名是否存在
        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        
        // 检查邮箱是否存在
        if (userMapper.existsByEmail(userDTO.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        userMapper.insert(user);
    }
}
```

### 5. 参数校验

#### 使用 @Valid 校验

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping
    public void createUser(@RequestBody @Valid UserDTO userDTO) {
        userService.create(userDTO);
    }

    @PutMapping("/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody @Valid UserDTO userDTO) {
        userService.update(id, userDTO);
    }
}

@Data
public class UserDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;
    
    @Min(value = 18, message = "年龄不能小于18岁")
    @Max(value = 100, message = "年龄不能大于100岁")
    private Integer age;
}
```

#### 使用 @Validated 校验

```java
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @GetMapping("/{id}")
    public User getUser(
            @PathVariable @Min(value = 1, message = "用户ID必须大于0") Long id) {
        return userService.getById(id);
    }

    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam @NotBlank(message = "关键词不能为空") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页数量必须大于0") Integer size) {
        return userService.search(keyword, page, size);
    }
}
```

### 6. XSS 防护

#### 自动防护

引入依赖后，XSS 防护自动生效，无需额外配置。

```java
@RestController
@RequestMapping("/api/article")
public class ArticleController {

    @PostMapping
    public void createArticle(@RequestBody ArticleDTO articleDTO) {
        // articleDTO.getContent() 已自动过滤 XSS
        articleService.create(articleDTO);
    }
}
```

#### 排除路径

对于富文本编辑器等场景，可以排除 XSS 过滤：

```yaml
hc:
  web:
    xss-enabled: true
    xss-exclude-urls:
      - /api/editor/**
      - /api/rich-text/**
      - /api/content/**
```

### 7. 使用工具类

```java
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 获取客户端 IP
        info.put("clientIP", ServletUtils.getClientIP());
        
        // 获取 User-Agent
        info.put("userAgent", ServletUtils.getUserAgent());
        
        // 获取请求对象
        HttpServletRequest request = ServletUtils.getRequest();
        info.put("requestURI", request.getRequestURI());
        
        return info;
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        ServletUtils.writeAttachment(response, "hello.txt", content);
    }
}
```

### 8. 自定义字段名

如果前端需要不同的字段名，可以自定义：

```yaml
hc:
  web:
    code-field: status        # 将 code 字段改为 status
    message-field: msg        # 将 message 字段改为 msg
    data-field: result        # 将 data 字段改为 result
```

响应示例：

```json
{
  "status": 200,
  "msg": "操作成功",
  "result": {
    "id": 1,
    "name": "张三"
  },
  "timestamp": "2026-04-07 10:30:00",
  "path": "/api/user/1"
}
```

## 配置说明

### 完整配置示例

```yaml
hc:
  web:
    enabled: true                    # 是否启用 Web Starter
    wrap-response: true              # 是否自动包装响应
    code-field: code                 # 响应码字段名
    message-field: message           # 响应消息字段名
    data-field: data                 # 响应数据字段名
    xss-enabled: true                # 是否启用 XSS 防护
    xss-exclude-urls:                # XSS 排除路径
      - /api/editor/**
      - /api/rich-text/**
      - /api/content/**
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `hc.web.enabled` | 是否启用 Web Starter | `true` |
| `hc.web.wrap-response` | 是否自动包装响应 | `true` |
| `hc.web.code-field` | 响应码字段名 | `code` |
| `hc.web.message-field` | 响应消息字段名 | `message` |
| `hc.web.data-field` | 响应数据字段名 | `data` |
| `hc.web.xss-enabled` | 是否启用 XSS 防护 | `true` |
| `hc.web.xss-exclude-urls` | XSS 排除路径 | 空 |

## 高级用法

### 1. 关闭响应自动包装

如果某些接口不需要自动包装，可以关闭：

```yaml
hc:
  web:
    wrap-response: false
```

或者使用 `@RawResponse` 注解（需要自定义实现）：

```java
@RestController
@RequestMapping("/api/raw")
public class RawController {

    @GetMapping("/text")
    public String getRawText() {
        return "This is raw text without wrapping";
    }
}
```

### 2. 自定义异常处理

可以扩展 `GlobalExceptionHandler` 添加自定义异常处理：

```java
@RestControllerAdvice
public class CustomExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public Result<Void> handleCustomException(CustomException e, HttpServletRequest request) {
        log.error("自定义异常: {}", e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }
}
```

### 3. 自定义 XSS 过滤规则

可以扩展 `XssKit` 添加自定义过滤规则：

```java
public class CustomXssKit extends XssKit {

    private static final Pattern CUSTOM_PATTERN = Pattern.compile(
        "(?i)custom-dangerous-pattern",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public static String escape(String value) {
        String result = super.escape(value);
        result = CUSTOM_PATTERN.matcher(result).replaceAll("");
        return result;
    }
}
```

### 4. 返回分页数据

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    public PageResult<User> pageUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return userService.page(page, size);
    }
}

@Data
public class PageResult<T> {
    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}
```

### 5. 文件上传下载

```java
@RestController
@RequestMapping("/api/file")
public class FileController {

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        String filePath = fileService.upload(file);
        return Result.success(filePath);
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileService.getById(id);
        byte[] content = fileService.getContent(id);
        ServletUtils.writeAttachment(response, fileInfo.getFileName(), content);
    }
}
```

## 常见问题

### 1. 如何返回不包装的原始数据？

有两种方式：

**方式一：关闭自动包装**

```yaml
hc:
  web:
    wrap-response: false
```

**方式二：返回 Result 类型**

```java
@GetMapping("/raw")
public Result<String> getRawData() {
    return Result.success("raw data");
}
```

### 2. 如何自定义错误码？

使用 `BusinessException` 自定义错误码：

```java
throw new BusinessException(1001, "自定义错误信息");
```

### 3. XSS 防护会影响富文本吗？

默认的 XSS 防护会过滤危险脚本，但保留正常的 HTML 标签。如果需要完全不过滤，可以配置排除路径：

```yaml
hc:
  web:
    xss-exclude-urls:
      - /api/editor/**
```

### 4. 如何处理文件上传？

XSS 防护只过滤字符串参数，不会影响文件上传。文件上传可以正常使用 `MultipartFile`。

### 5. 如何获取请求体？

使用 `ServletUtils.getBody()`：

```java
@PostMapping("/raw")
public void handleRawBody(HttpServletRequest request) {
    String body = ServletUtils.getBody(request);
    // 处理请求体
}
```

## 最佳实践

### 1. 统一响应格式

- 所有接口返回统一的 Result 格式
- 使用自动包装减少重复代码
- 错误码使用 HTTP 状态码或自定义业务码

### 2. 异常处理

- 使用 BusinessException 抛出业务异常
- 不要在业务代码中捕获异常后返回 Result.error()
- 让全局异常处理器统一处理异常

### 3. 参数校验

- 使用 @Valid/@Validated 进行参数校验
- 在 DTO 中定义校验规则
- 提供友好的错误提示信息

### 4. XSS 防护

- 默认开启 XSS 防护
- 只对富文本等特殊场景排除过滤
- 不要完全关闭 XSS 防护

### 5. 性能优化

- 避免在循环中调用 ServletUtils.getRequest()
- 合理使用缓存
- 异步处理耗时操作

## 技术支持

- Spring Boot 官方文档：https://spring.io/projects/spring-boot
- 项目源码：[hc-spring-cloud-framework](https://github.com/your-repo/hc-spring-cloud-framework)
- 问题反馈：[Issues](https://github.com/your-repo/hc-spring-cloud-framework/issues)

## 版本历史

- **1.0.0**：初始版本，提供统一响应格式、全局异常处理、XSS 防护、响应自动包装等功能
