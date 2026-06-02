# hc-web-spring-boot-starter

## 模块简介

`hc-web-spring-boot-starter` 是 Spring Boot Web 应用的基础增强 Starter，为微服务项目提供统一的 Web 层解决方案。

本模块封装了 Spring Boot Web 开发的常用功能，提供了统一响应格式、全局异常处理、XSS 防护、响应自动包装等特性，简化 Web 层开发，提高开发效率。

## 设计思路

### 核心设计理念

1. **统一响应格式**：所有接口返回统一的 Result 格式，便于前端统一处理
2. **自动包装**：通过 `ResponseBodyAdvice` 自动包装返回值，减少重复代码
3. **全局异常处理**：统一处理各类异常，返回标准错误响应
4. **XSS 双重防护**：通过 `Filter`（请求参数）+ `Jackson` 反序列化器（JSON Body）双重防护
5. **动态字段名**：支持自定义响应字段名（`code`/`message`/`data`），适配不同的前端 API 规范
6. **开箱即用**：引入依赖后自动配置，零配置即可使用

### 架构设计

```
hc-web-spring-boot-starter
├── config/                          # 自动配置
│   ├── WebAutoConfiguration         # 核心自动配置（@PostConstruct 注册 Jackson Module）
│   └── WebProperties                # 配置属性类
├── exception/                       # 异常处理
│   ├── BusinessException            # 业务异常
│   └── GlobalExceptionHandler       # 全局异常处理器（8 种异常类型）
├── model/                           # 模型
│   └── Result                       # 统一响应结果（继承 common.Result）
├── serializer/                      # 序列化
│   └── ResultSerializer             # Result 动态字段名序列化器
├── util/                            # 工具类
│   └── ServletUtils                 # Servlet 工具类
├── wrapper/                         # 包装器
│   ├── CustomizeRequestWrapper      # 请求包装器（支持 body 重复读取、header 增删）
│   └── ResponseWrapAdvice           # 响应自动包装（ResponseBodyAdvice）
└── xss/                             # XSS 防护
    ├── XssFilter                    # XSS 过滤器（Filter 层，过滤请求参数）
    ├── XssHttpServletRequestWrapper # XSS 请求包装器（包装 getParameter 系列方法）
    ├── XssKit                       # XSS 过滤核心工具（正则 + 迭代清理）
    └── XssStringDeserializer        # XSS 字符串反序列化器（Jackson 层，过滤 JSON Body）
```

### 核心流程

#### 1. 请求处理流程

```
客户端请求
    ↓
XssFilter 过滤
    ├── 放行路径？→ 直接通过
    └── 否则 → XssHttpServletRequestWrapper 包装
              └── getParameter() / getParameterValues() / getParameterMap() → XSS 过滤
              └── getHeader() / getQueryString() / getAttribute() → 直接透传（不过滤）
    ↓
Controller 处理
    ├── JSON Body 反序列化 → XssStringDeserializer 过滤所有 String 字段
    └── 业务逻辑
    ↓
返回数据
    ↓
ResponseWrapAdvice 拦截
    ├── wrapResponse=true 且返回值非 Result → 自动包装为 Result.success(data)
    ├── StringHttpMessageConverter 文本响应 → 跳过包装
    └── 已是 Result 类型 → 跳过包装
    ↓
ResultSerializer 序列化（动态字段名 code/message/data + timestamp + path）
    ↓
返回 JSON 给客户端
```

#### 2. 异常处理流程

```
Controller 抛出异常
    ↓
GlobalExceptionHandler 捕获
    ↓
┌─────────────────────────────────────────────────────┐
│ 异常类型                       → HTTP 状态码          │
│ RepeatSubmitException          → 429 Too Many Requests│
│ BusinessException              → 业务自定义 code       │
│ MethodArgumentNotValidException→ 400 Bad Request      │
│ BindException                  → 400 Bad Request      │
│ ConstraintViolationException   → 400 Bad Request      │
│ MissingServletRequestParameter → 400 Bad Request      │
│ MethodArgumentTypeMismatch     → 400 Bad Request      │
│ IllegalArgumentException       → 400 Bad Request      │
│ IllegalStateException          → 500 Internal Error   │
│ Exception（兜底）               → 500 Internal Error   │
└─────────────────────────────────────────────────────┘
    ↓
构建 Result 错误响应（含 path）
    ↓
返回 JSON 给客户端
```

#### 3. XSS 防护分层

```
┌─────────────────────────────────────────────────────┐
│ 防护层 1: Filter 层（XssFilter）                      │
│ 范围：URL 请求参数（getParameter 系列方法）              │
│ 不过滤：Header、QueryString、Attribute                │
│ 特点：对业务代码透明，无需额外注解                        │
├─────────────────────────────────────────────────────┤
│ 防护层 2: Jackson 层（XssStringDeserializer）          │
│ 范围：JSON Body 反序列化时所有 String 字段               │
│ 特点：全局生效，在 Jackson 反序列化阶段过滤               │
│ 如需按字段禁用：@JsonDeserialize(using = None.class)    │
└─────────────────────────────────────────────────────┘
```

#### 4. ObjectMapper 架构

```
Spring 托管 ObjectMapper（唯一的 ObjectMapper 实例）
    │
    ├── @PostConstruct → registerModule(SimpleModule)
    │       ├── ResultSerializer  （仅匹配 Result 类型）
    │       └── XssStringDeserializer（仅匹配 String 类型）
    │
    └── 所有 MappingJackson2HttpMessageConverter 共享
        无需 copy()，无额外 Converter 插入
```

---

## 功能特性

### 1. 统一响应格式

- **标准 Result 结构**：包含 `code`、`message`、`data`、`timestamp`、`path` 字段
- **静态工厂方法**：提供 `Result.success()`、`Result.error()` 等便捷方法
- **泛型支持**：`Result<T>` 支持任意类型的数据
- **时间戳自动生成**：构造时自动添加 `LocalDateTime.now()`

### 2. 响应自动包装

- **自动包装**：通过 `ResponseWrapAdvice`（`ResponseBodyAdvice`）自动将非 Result 返回值包装为 `Result.success(data)`
- **开关控制**：`hc.web.wrap-response=false` 关闭自动包装
- **智能排除**：已是 `Result` 类型不包装；`StringHttpMessageConverter` 文本响应不包装
- **用户可覆盖**：注册自己的 `ResponseWrapAdvice` Bean 即可替换默认实现

### 3. 全局异常处理

- **8 种异常精确处理** + 1 个兜底处理，全部返回统一 `Result` 格式
- **参数校验异常**：`@Valid`/`@Validated` 校验失败自动提取 `FieldError` 拼接消息
- **用户可扩展**：注册自己的 `GlobalExceptionHandler` Bean 即可替换（`@ConditionalOnMissingBean`）

### 4. XSS 防护

- **Filter 层**：过滤 URL 请求参数（`getParameter` 系列），不过滤 Header/QueryString/Attribute
- **Jackson 层**：JSON Body 反序列化时自动过滤所有 String 字段
- **智能过滤**：`XssKit.escape()` 只移除 `<script>`、`<iframe>`、`onclick=` 等危险模式，保留正常 HTML 字符（如 `a < b`）
- **排除路径**：支持 Ant 风格路径排除富文本编辑器等场景
- **两种模式**：`escape()` 智能模式 + `escapeStrict()` 严格模式（转义所有 HTML 实体）

### 5. 动态字段名

- **自定义字段名**：通过 `hc.web.code-field`、`hc.web.message-field`、`hc.web.data-field` 自定义
- **Jackson 集成**：`ResultSerializer` 在序列化时动态替换字段名
- **timestamp/path 不受影响**：始终使用固定字段名

### 6. 工具类

- **`ServletUtils`**：
  - `getRequest()` — 获取当前请求对象
  - `getClientIP()` / `getClientIP(request)` — 获取客户端 IP
  - `getUserAgent()` / `getUserAgent(request)` — 获取 User-Agent
  - `writeJSON(response, object)` — 返回 JSON 响应
  - `writeAttachment(response, filename, content)` — 返回附件
  - `getBody(request)` / `getBodyBytes(request)` — 获取请求体
  - `getParamMap(request)` — 获取请求参数 Map
  - `isJsonRequest(request)` — 判断是否 JSON 请求

### 7. 请求包装器

- **`CustomizeRequestWrapper`**：
  - 缓存 `body` 支持重复读取（解决 `HttpServletRequest` 流不可重复读的痛点）
  - 支持动态添加/删除 Header（`addHeader`/`removeHeader`）
  - Header 名大小写不敏感（统一小写存储）

---

## 集成使用方法

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-web-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加（全部可选，均有默认值）：

```yaml
hc:
  web:
    enabled: true                    # 是否启用（默认 true）
    wrap-response: true              # 是否自动包装响应（默认 true）
    code-field: code                 # 响应码字段名（默认 code）
    message-field: message           # 响应消息字段名（默认 message）
    data-field: data                 # 响应数据字段名（默认 data）
    xss-enabled: true                # 是否启用 XSS 防护（默认 true）
    xss-exclude-urls:                # XSS 排除路径（Ant 风格）
      - /api/editor/**
      - /api/rich-text/**
```

### 3. 使用统一响应格式

#### 方式一：手动返回 Result（关闭自动包装时推荐）

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    @PostMapping
    public Result<Void> createUser(@RequestBody @Valid UserDTO dto) {
        userService.create(dto);
        return Result.success("创建成功");
    }
}
```

#### 方式二：自动包装（默认开启，推荐）

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // 自动包装为 Result.success(user)
        return userService.getById(id);
    }

    @PostMapping
    public void createUser(@RequestBody @Valid UserDTO dto) {
        // 自动包装为 Result.success()（data=null）
        userService.create(dto);
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        // 自动包装为 Result.success(list)
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
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user/1"
}
```

### 4. 使用业务异常

```java
@Service
public class UserServiceImpl implements UserService {

    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");          // code=500
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");     // 自定义 code
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return user;
    }

    public void create(UserDTO dto) {
        if (userMapper.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        // ... 业务逻辑
    }
}
```

业务异常响应：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user/login"
}
```

### 5. 参数校验

#### @Valid 校验（DTO 字段）

```java
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
}

@RestController
@RequestMapping("/api/user")
public class UserController {

    @PostMapping
    public void createUser(@RequestBody @Valid UserDTO dto) {
        // 校验失败自动返回 400 + 错误消息
        userService.create(dto);
    }
}
```

#### @Validated 校验（方法参数）

```java
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        return userService.getById(id);
    }

    @GetMapping("/search")
    public List<User> search(
            @RequestParam @NotBlank(message = "关键词不能为空") String keyword,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer size) {
        return userService.search(keyword, page, size);
    }
}
```

校验失败响应（自动拼接所有字段的错误消息）：

```json
{
  "code": 400,
  "message": "用户名不能为空, 邮箱格式不正确",
  "data": null,
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user"
}
```

### 6. XSS 防护

#### 自动防护（默认）

引入依赖后，XSS 防护自动生效：

- **请求参数**：`getParameter("content")` 自动过滤 XSS
- **JSON Body**：`@RequestBody` 反序列化时所有 String 字段自动过滤

```java
@RestController
@RequestMapping("/api/article")
public class ArticleController {

    @PostMapping
    public void createArticle(@RequestBody ArticleDTO dto) {
        // dto.getTitle() 和 dto.getContent() 已自动过滤 XSS
        articleService.create(dto);
    }
}
```

#### 排除路径（富文本场景）

```yaml
hc:
  web:
    xss-enabled: true
    xss-exclude-urls:
      - /api/editor/**
      - /api/rich-text/**
```

#### 按字段禁用 XSS（JSON Body 场景）

`XssStringDeserializer` 全局注册在所有 String 字段上。如果某个字段需要原始值（如密码），可通过 Jackson 注解覆盖：

```java
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

@Data
public class LoginDTO {
    @NotBlank
    private String username;     // ← 自动 XSS 过滤

    @NotBlank
    @JsonDeserialize(using = StringDeserializer.class)  // ← 禁用 XSS 过滤
    private String password;
}
```

#### 手动使用 XssKit

```java
// 智能模式：只移除危险脚本，保留正常 HTML 字符
String safe = XssKit.escape("<p>hello</p><script>alert(1)</script>");
// 结果: "<p>hello</p>"（<script> 被移除，<p> 保留）

// 严格模式：转义所有 HTML 特殊字符
String strict = XssKit.escapeStrict("<p>a < b</p>");
// 结果: "&lt;p&gt;a &lt; b&lt;/p&gt;"（全部转义）
```

### 7. 使用工具类

```java
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("clientIP", ServletUtils.getClientIP());
        info.put("userAgent", ServletUtils.getUserAgent());
        info.put("requestURI", ServletUtils.getRequest().getRequestURI());
        return info;
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        byte[] content = fileService.getContent();
        ServletUtils.writeAttachment(response, "report.pdf", content);
    }
}
```

### 8. 自定义字段名

```yaml
hc:
  web:
    code-field: status        # code → status
    message-field: msg        # message → msg
    data-field: result        # data → result
```

响应示例：

```json
{
  "status": 200,
  "msg": "操作成功",
  "result": {"id": 1, "name": "张三"},
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user/1"
}
```

---

## 配置说明

### 完整配置

```yaml
hc:
  web:
    enabled: true                    # 是否启用 Web Starter（默认 true）
    wrap-response: true              # 是否自动包装响应（默认 true）
    code-field: code                 # 响应码字段名（默认 code）
    message-field: message           # 响应消息字段名（默认 message）
    data-field: data                 # 响应数据字段名（默认 data）
    xss-enabled: true                # 是否启用 XSS 防护（默认 true）
    xss-exclude-urls:                # XSS 排除路径，Ant 风格（默认空）
      - /api/editor/**
      - /api/rich-text/**
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `hc.web.enabled` | `Boolean` | `true` | 总开关，关闭后所有功能失效 |
| `hc.web.wrap-response` | `Boolean` | `true` | 是否自动将非 Result 返回值包装为 `Result.success()` |
| `hc.web.code-field` | `String` | `code` | 响应码 JSON 字段名 |
| `hc.web.message-field` | `String` | `message` | 响应消息 JSON 字段名 |
| `hc.web.data-field` | `String` | `data` | 响应数据 JSON 字段名 |
| `hc.web.xss-enabled` | `Boolean` | `true` | XSS 防护开关 |
| `hc.web.xss-exclude-urls` | `List<String>` | 空 | 不进行 XSS 过滤的 URL 路径（Ant 风格） |

---

## 高级用法

### 1. 自定义全局异常处理器

利用 `@ConditionalOnMissingBean`，只需注册自己的 `GlobalExceptionHandler` Bean 即可完全替换默认实现：

```java
@Slf4j
@RestControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }

    // 自定义异常
    @ExceptionHandler(MyCustomException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleCustom(MyCustomException e, HttpServletRequest request) {
        Result<Void> result = Result.error(502, e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        Result<Void> result = Result.error(500, "系统繁忙");
        result.setPath(request.getRequestURI());
        return result;
    }
}
```

### 2. 自定义响应包装逻辑

```java
@RestControllerAdvice
public class MyResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    private final WebProperties webProperties;

    public MyResponseWrapAdvice(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 自定义判断逻辑
        return webProperties.getWrapResponse()
            && !returnType.getParameterType().isAssignableFrom(Result.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }
        return Result.success(body);
    }
}
```

### 3. 返回分页数据

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/page")
    public PageResult<User> pageUsers(PageParam param) {
        // 自动包装：Result.success(pageResult)
        return userService.page(param);
    }
}

@Data
public class PageResult<T> {
    private List<T> records;
    private Long total;
    private Long page;
    private Long size;
}
```

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  },
  "timestamp": "2026-06-02 10:30:00",
  "path": "/api/user/page"
}
```

### 4. 文件上传与下载

```java
@RestController
@RequestMapping("/api/file")
public class FileController {

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // XSS 防护不影响文件上传（只过滤字符串参数）
        String url = fileService.upload(file);
        return Result.success("上传成功", url);
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        FileInfo info = fileService.getById(id);
        byte[] content = fileService.getContent(id);
        ServletUtils.writeAttachment(response, info.getFileName(), content);
    }
}
```

### 5. 使用 CustomizeRequestWrapper 处理 Body 重复读取

```java
// 场景：需要在 Filter 中读取请求体，但 Controller 也需要读取

@WebFilter("/*")
public class SignFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        CustomizeRequestWrapper wrapper = new CustomizeRequestWrapper((HttpServletRequest) request);
        String body = new String(wrapper.getBodyBytes(), StandardCharsets.UTF_8);
        // 验签逻辑...
        chain.doFilter(wrapper, response);
    }
}
```

---

## 常见问题

### 1. 如何返回不包装的原始数据？

三种方式：

| 方式 | 适用场景 |
|------|----------|
| `hc.web.wrap-response=false` | 全局关闭自动包装 |
| 返回值声明为 `Result<T>` | 单个接口跳过包装 |
| 返回类型使用 `StringHttpMessageConverter`（如 `text/plain`） | 返回纯文本/HTML |

### 2. 如何自定义错误码？

```java
throw new BusinessException(1001, "自定义错误信息");
// 响应: {"code": 1001, "message": "自定义错误信息", ...}
```

### 3. XSS 防护会影响富文本吗？

默认的 `XssKit.escape()` 只移除危险标签（`<script>`、`<iframe>`、`onclick=` 等），**保留** `<p>`、`<br>`、`<b>` 等正常 HTML 标签。

如果使用了 `XssKit.escapeStrict()`（严格模式），则会转义所有 HTML。建议对富文本场景使用**排除路径**：

```yaml
hc:
  web:
    xss-exclude-urls:
      - /api/editor/**
```

### 4. XSS 防护会过滤 HTTP Header 和 Cookie 吗？

**不会**。`XssHttpServletRequestWrapper` 只过滤 `getParameter()` / `getParameterValues()` / `getParameterMap()`，不过滤 `getHeader()` / `getQueryString()` / `getAttribute()`。Authorization、Cookie 等 Header 完全不受影响。

### 5. 如何对单个 JSON 字段禁用 XSS 过滤？

```java
@JsonDeserialize(using = com.fasterxml.jackson.databind.deser.std.StringDeserializer.class)
private String rawContent;  // 不会被 XSS 过滤
```

### 6. 如何处理文件上传和 XSS？

XSS 防护只作用于字符串参数，不影响 `MultipartFile`。文件上传完全不受影响。

### 7. 如何获取请求体（多次读取）？

```java
String body = ServletUtils.getBody(request);       // String
byte[] bytes = ServletUtils.getBodyBytes(request);  // byte[]
```

### 8. IllegalStateException 为什么返回 500？

`IllegalStateException` 通常表示服务端内部状态异常（如配置缺失、Bean 未初始化），属于服务端错误。客户端请求触发的非法状态应使用 `IllegalArgumentException`（返回 400）。

---

## 最佳实践

### 1. 统一响应格式

- ✅ 开启自动包装，Controller 直接返回业务对象
- ✅ 错误场景使用 `BusinessException` 抛出，不要手动 `return Result.error()`
- ✅ 状态码遵循 HTTP 标准（200/400/401/403/404/500）

### 2. 异常处理

- ✅ 使用 `BusinessException` 抛出业务异常
- ❌ 不要在 Controller 中 try-catch 后手动返回 `Result.error()`（除非有特殊处理逻辑）
- ✅ 自定义异常处理器时注册自己的 `GlobalExceptionHandler` Bean（会替换默认实现）

### 3. 参数校验

- ✅ 使用 `@Valid` + JSR-303 注解在 DTO 中声明校验规则
- ✅ 简单校验用 `@Validated` + 方法参数注解
- ✅ 错误消息要友好、明确（如"用户名长度必须在3-20之间"而非"校验失败"）

### 4. XSS 防护

- ✅ 保持默认开启
- ✅ 富文本编辑器路径配置排除
- ✅ 对密码等不需要过滤的 JSON 字段使用 `@JsonDeserialize(using = StringDeserializer.class)`
- ❌ 不要完全关闭 XSS 防护

### 5. 性能

- 避免在循环中调用 `ServletUtils.getRequest()`
- `CustomizeRequestWrapper` 会缓存 body 字节数组，大文件上传场景注意内存使用

---

## 异常处理速查表

| 异常类型 | HTTP 状态码 | 日志级别 | 说明 |
|----------|------------|---------|------|
| `RepeatSubmitException` | 429 | WARN | 重复提交检测（来自 hc-redis 模块） |
| `BusinessException` | 自定义 code | WARN | 业务异常，code 由构造参数决定 |
| `MethodArgumentNotValidException` | 400 | WARN | `@Valid`/`@Validated` 校验失败 |
| `BindException` | 400 | WARN | 参数绑定失败 |
| `ConstraintViolationException` | 400 | WARN | `@RequestParam` 校验失败 |
| `MissingServletRequestParameterException` | 400 | WARN | 缺少必要参数 |
| `MethodArgumentTypeMismatchException` | 400 | WARN | 参数类型不匹配 |
| `IllegalArgumentException` | 400 | WARN | 非法参数（业务校验结果） |
| `IllegalStateException` | 500 | ERROR | 服务端内部状态错误 |
| `Exception`（兜底） | 500 | ERROR | 未预期的系统异常，返回"系统繁忙" |

---

## 版本历史

- **1.0.0**：初始版本，提供统一响应格式、全局异常处理、XSS 双重防护、响应自动包装、动态字段名、Servlet 工具类
