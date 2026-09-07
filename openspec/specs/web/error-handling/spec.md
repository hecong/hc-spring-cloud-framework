# Error Handling Specification

## Purpose

统一业务异常与常见 HTTP/客户端错误到标准错误响应的语义：业务异常默认归属 4xx；方法/媒体类型/上传大小/路径变量等客户端错误以正确 HTTP 状态码与统一 Result 结构返回。

## Requirements

### Requirement: 业务异常默认错误码 4xx

系统使用单参数构造的业务异常时，默认错误码 SHALL 为 400（BAD_REQUEST 语义）；显式指定错误码的构造不受影响。

#### Scenario: 单参构造默认 400

- **WHEN** 业务代码 `new BusinessException("xx")`
- **THEN** 异常的错误码为 400（而非 500）

#### Scenario: 显式错误码不受影响

- **WHEN** 业务代码 `new BusinessException(500, "xx")` 或 `new BusinessException(20001, "xx")`
- **THEN** 错误码按显式值保留

#### Scenario: 默认业务异常响应为 400 语义

- **WHEN** 单参构造的业务异常被全局异常处理器捕获
- **THEN** 响应体中的错误码为 400（配合前端语义调整，README 发布说明已声明）

### Requirement: 方法不支持映射为 405

系统在请求方法不受接口支持时 SHALL 返回 405（HttpRequestMethodNotSupported），响应为统一 Result 结构，消息为固定中文文案（不含内部细节）。

#### Scenario: 方法不支持返回 405

- **WHEN** 客户端以接口不支持的方法（如 `DELETE`）访问仅允许 `GET`/`POST` 的路径
- **THEN** 收到 HTTP 405 且响应体为统一错误结构（错误码 405、固定消息、请求路径）

### Requirement: Content-Type 不支持映射为 415

系统在请求体媒体类型不受接口支持时 SHALL 返回 415（HttpMediaTypeNotSupported），响应为统一 Result 结构，消息固定不泄漏细节。

#### Scenario: 媒体类型不支持返回 415

- **WHEN** 客户端以接口不接受的 `Content-Type`（如 `text/plain` 请求要求 JSON 的接口）调用
- **THEN** 收到 HTTP 415 且响应体为统一错误结构（错误码 415、固定消息）

### Requirement: 上传超限映射为 413

系统在请求体上传大小超限（如 `MaxUploadSizeExceededException`）时 SHALL 返回 413，消息使用固定文案（不暴露服务器内部阈值或细节），响应为统一 Result 结构。

#### Scenario: 上传超限返回 413

- **WHEN** 上传文件/请求体超过服务器限制
- **THEN** 收到 HTTP 413 且响应体为统一错误结构（错误码 413、固定提示文案）

### Requirement: 缺少路径变量映射为 400

系统在 `@PathVariable` 声明的路径变量缺失时 SHALL 返回 400（`MissingPathVariableException`），消息说明缺失的路径变量名，响应为统一 Result 结构。

#### Scenario: 路径变量缺失返回 400

- **WHEN** 请求路径缺少接口声明必需的路径变量
- **THEN** 收到 HTTP 400 且响应体含缺失路径变量名的提示（统一错误结构）
