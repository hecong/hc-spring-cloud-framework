# hc-spring-cloud-framework

Spring Cloud 微服务基础框架：`hc-common` / `hc-web` / `hc-logging` / `hc-mybatis-plus` / `hc-excel` /
`hc-redis` / `hc-oss` / `hc-rocketmq` / `hc-satoken` / `hc-satoken-gateway` 等可复用 `spring-boot-starter`
的多模块 Maven 工程。各模块功能、配置与用法见模块内 `README.md`。

## 工程坐标（groupId 统一）

本项目 **groupId 统一为 `com.hc.framework`**，与代码包名 `com.hc.framework` 对齐；`version = 1.0-SNAPSHOT`。

### 坐标对照（发布说明 · 自本版本起）

> 自本版本起框架坐标完成统一，**旧坐标 `com.hnhegui.framework` 停止发布新版本**（版本历史见各模块 README）。
> 若需要平滑过渡，发布流程可按需补发**旧坐标空壳 artifact**（仅包含 pom，依赖指向新坐标），
> 仓库内不包含空壳模块；不补发空壳时，业务侧需在升级窗口内一次性切换。

| 项 | 旧坐标（已停发） | 新坐标（当前） |
|---|---|---|
| groupId | `com.hnhegui.framework` | `com.hc.framework` |
| artifactId / version | 各模块 artifactId 与 version **不变** | 同左 |

### 业务工程切换步骤

1. 全局替换依赖坐标：`com.hnhegui.framework` → `com.hc.framework`（artifactId、version 不变）。
   以 `hc-common-spring-boot-starter` 为例：

```xml
<!-- 迁移前 -->
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-common-spring-boot-starter</artifactId>
</dependency>

<!-- 迁移后 -->
<dependency>
    <groupId>com.hc.framework</groupId>
    <artifactId>hc-common-spring-boot-starter</artifactId>
</dependency>
```

2. 若业务工程使用 Spring Boot BOM / dependencyManagement 统一托管本框架版本，同步更新其中 groupId。
3. 清理 IDE 索引/Maven 本地仓库缓存后重新导入（`mvn clean verify` 验证依赖解析）。
4. 代码包 `com.hc.framework.*` 不变，**无需改动任何 Java 代码**；仅配置类 IDE 提示能力增强（见下节）。

### IDE 配置提示（configuration-processor）

全部 10 个模块统一引入 `spring-boot-configuration-processor`（optional），编译期自动生成
`META-INF/spring-configuration-metadata.json`，配置类前缀均为 `hc.*`（如 `hc.logging`、`hc.web`、`hc.redis`、
`hc.excel`、`hc.oss`、`hc.satoken` 等）。效果：

- IDE 在 `application.yml/properties` 中针对 `hc.*` 配置自动**补全、跳转与校验**；
- 打包后的 starter 内含 metadata，业务工程引入后同样生效；
- processor 为 optional，不会传递到业务工程运行期 classpath。

### StringUtils 委托化声明（API 不变）

`hc-common` 的 `StringUtils` 对 hutool6（`org.dromara.hutool`）**语义等价**的通用方法改为一行委托
（`isBlank/isNotBlank` → `StrUtil`，`isEmpty(Collection/Map)` → `CollUtil`/`MapUtil`，
`defaultIfBlank/nullToEmpty/capitalize` → `StrUtil`），**公开 API 与签名不变，业务调用零迁移**。要点：

- **空白语义对齐**：委托后 `isBlank` 系列将 NBSP（U+00A0）、BOM（U+FEFF）判为 blank（相对旧
  `String.isBlank` 为期望的超集对齐，与框架内既有 `StrUtil` 用法一致）；零宽空格 U+200B 判定不变。
- **保留原实现**（与 hutool6 语义不等价或属框架特色）：`format`、`camelToUnderscore`、`truncate`、
  `maskMobile/maskEmail/maskIdCard`、`isMobile/isEmail`、`splitAndTrim`。
- 每处委托/保留依据与边界说明已写入方法 Javadoc 及 `StringUtilsTest` 契约用例。

## 技术栈与约定

- 各 starter 均通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  注册自动配置；模块间依赖经根 pom `dependencyManagement` 统一管理。
- 示例/演示代码一律置于 `src/test`（如 `hc-rocketmq` 的 `example` 包），禁止进入 `src/main`。
