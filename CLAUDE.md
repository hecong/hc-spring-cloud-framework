# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build entire project (run from hc-spring-cloud-framework/ directory)
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Build a single module
mvn clean install -pl hc-web-spring-boot-starter

# Run tests for a single module
mvn test -pl hc-excel-spring-boot-starter

# Run a single test class
mvn test -pl hc-excel-spring-boot-starter -Dtest=ExcelExportServiceImplTest
```

**Java 17** required. No wrapper scripts checked in; Maven must be installed locally.

## Architecture

This is a **multi-module Maven project** (`com.hnhegui.framework`, version `1.0-SNAPSHOT`) providing reusable Spring Boot 3.3.0 / Spring Cloud 2023.0.2 starters for microservice development. Each module is an independent `spring-boot-starter` with its own auto-configuration registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3.x mechanism, not `spring.factories`).

### Module Dependency Graph

Actual dependencies declared in each module's `pom.xml`:

```
hc-common-spring-boot-starter   ← no internal deps (leaf module, no auto-config)
hc-web-spring-boot-starter      ← no internal deps
hc-redis-spring-boot-starter    ← no internal deps
hc-excel-spring-boot-starter    ← no internal deps
hc-oss-spring-boot-starter      ← no internal deps
hc-logging-spring-boot-starter  ← depends on hc-web, hc-common
hc-mybatis-plus-spring-boot-starter ← depends on hc-common, hc-web
hc-satoken-spring-boot-starter  ← depends on hc-web, hc-common, hc-redis
hc-rocketmq-spring-boot-starter ← depends on hc-common; hc-redis and hc-logging as optional
hc-satoken-gateway-spring-boot-starter ← hc-common and hc-redis as optional (reactive, for Gateway only)
```

Key takeaway: **5 out of 10 modules have zero internal framework dependencies**. They are truly standalone starters that only depend on third-party libraries. `hc-satoken` is the most "integrated" module, depending on three other framework modules.

### Module Purposes

| Module | Purpose | Config Prefix |
|---|---|---|
| `hc-common` | Shared constants, models, and utils (AssertUtils, DateUtils, JsonUtils, IpUtils, StringUtils, PageUtils). No auto-config class — pure library. | none |
| `hc-web` | Unified `Result<T>` response, `@BusinessException`, global exception handler, XSS filter + Jackson deserializer, `ResponseWrapAdvice` auto-wrapping. | `hc.web.*` |
| `hc-redis` | Redis caching with custom TTL in cache names (`cacheName#300s`), Redisson-based distributed locks (`LockTemplate`), `@RepeatSubmit` anti-duplicate annotation. | `hc.cache.*` |
| `hc-mybatis-plus` | `BaseEntity` (id, createTime, updateTime, deleted, creator, updater), `BaseService`/`BaseServiceImpl` with batch ops, unified `PageParam`/`PageResult`, dynamic datasource, MyBatis-Plus code generator. | `hc.mybatis-plus.*` |
| `hc-satoken` | Servlet-stack auth via Sa-Token: token management, `@SaCheckPermission`/`@SaCheckRole`, JWT mode, SSO, password encoder (BCrypt/MD5/SM3), permission caching, token cleanup scheduler. **Depends on hc-web, hc-common, hc-redis.** | `hc.satoken.*` |
| `hc-satoken-gateway` | Reactive Gateway auth: config-driven path-based auth rules, dynamic refresh from Nacos/Apollo/Polaris, `SaGatewayPermissionProvider` SPI for remote permission loading. **Reactor/WebFlux based, not servlet.** | `hc.satoken.gateway.*` |
| `hc-logging` | API logging aspect (request/response with sensitive param masking), `@RateLimiter` annotation backed by Sentinel, trace ID propagation across Feign + RestTemplate + HTTP requests, Logback config with dev/test/prod profiles. | `hc.logging.*` |
| `hc-excel` | EasyExcel-based import/export with async export via thread pool, multi-sheet import, dynamic headers, pivot tables, template export, pluggable task storage (local filesystem or Redis). | `hc.excel.*` |
| `hc-oss` | Unified object storage interface (`OssService`) with Aliyun OSS, MinIO, and Tencent COS implementations, selected by config. | `hc.oss.*` |
| `hc-rocketmq` | RocketMQ 5.x gRPC client: `BaseMqConsumer` base class, `RocketMqSender` for sending, transaction message support, idempotent consumption via Redis, MDC trace context propagation. | `hc.rocketmq.*` |

### Key Design Patterns

- **All modules have an `enabled` toggle** — set `<prefix>.enabled=false` to disable auto-configuration.
- **Config properties use `@ConfigurationProperties(prefix = "hc.<module>")`** — see each module's `*Properties` class for the full schema.
- **`hc-common` is a pure utility jar** with no auto-configuration — it does NOT have an `AutoConfiguration.imports` file. All other modules auto-register via that file.
- **SPI extension points**: `SaPermissionProvider` (satoken), `SaGatewayPermissionProvider` (satoken-gateway), `ExcelOperationRecorder`/`ExcelFileStorage`/`ExcelTaskStore` (excel) — implement these interfaces to customize behavior.
- **Inter-module dependencies are optional where used** — `hc-rocketmq` depends on `hc-redis` and `hc-logging` as `<optional>true</optional>`; `hc-satoken-gateway` depends on `hc-common` and `hc-redis` as `<optional>true</optional>`. Five modules (`hc-web`, `hc-redis`, `hc-excel`, `hc-oss`, `hc-common`) have zero internal dependencies at all.

### Technology Stack

- **Spring Boot 3.3.0**, **Spring Cloud 2023.0.2**, **Java 17**
- **Hutool 6.0.0-M10** (core utility library used throughout)
- **Fastjson2** (JSON), **Redisson** (distributed locks), **EasyExcel 4.0.1**, **MyBatis-Plus 3.5.6**, **Sa-Token 1.45.0**, **Sentinel 1.8.8**
- **Lombok** used extensively — all `@Data` on POJOs, `@RequiredArgsConstructor` for DI
