# hc-mybatis-plus-spring-boot-starter

## 模块简介

`hc-mybatis-plus-spring-boot-starter` 是基于 MyBatis-Plus 的增强 Starter，为 Spring Boot 项目提供了丰富的 MyBatis-Plus 功能增强和简化配置。

本模块封装了 MyBatis-Plus 的常用功能，提供了自动配置、动态数据源、统一分页、自动填充、乐观锁等特性，简化 MyBatis-Plus 的使用，提高开发效率。

## 设计思路

### 核心设计理念

1. **开箱即用**：引入依赖后自动配置，零配置即可使用
2. **功能增强**：在 MyBatis-Plus 基础上提供额外的功能增强
3. **统一规范**：提供统一的实体基类、Service 接口和分页模型
4. **性能优化**：提供批量操作、动态数据源等性能优化特性
5. **安全性**：内置防止全表更新/删除等安全措施

### 架构设计

```
hc-mybatis-plus-spring-boot-starter
├── config/                      # 配置类
│   ├── MyBatisPlusConfig         # MyBatis-Plus 核心配置
│   └── DynamicDataSourceConfig   # 动态数据源配置
├── entity/                      # 实体类
│   └── BaseEntity                # 基础实体类
├── generator/                   # 代码生成
│   └── CodeGenerator             # 代码生成器
├── handler/                     # 处理器
│   └── DefaultMetaObjectHandler  # 自动填充处理器
├── model/                       # 模型
│   ├── PageParam                 # 分页参数
│   └── PageResult                # 分页结果
├── properties/                  # 配置属性
│   └── MyBatisPlusProperties     # 配置属性类
└── service/                     # 服务层
    ├── BaseService               # 基础 Service 接口
    └── BaseServiceImpl           # 基础 Service 实现
```

### 核心流程

#### 1. 自动配置流程

```
应用启动
    ↓
Spring Boot 自动配置
    ↓
MyBatisPlusConfig 生效
    ↓
注册 MyBatis-Plus 插件
    ↓
注册自动填充处理器
    ↓
DynamicDataSourceConfig 生效（如果启用）
    ↓
应用可使用 MyBatis-Plus 功能
```

#### 2. 动态数据源流程

```
业务代码
    ↓
DataSourceContext.switchTo(dataSource)
    ↓
执行数据库操作
    ↓
DataSourceContext.clear()
```

#### 3. 分页查询流程

```
Controller 接收分页参数
    ↓
构建 PageParam 对象
    ↓
调用 service.pageResult(pageParam)
    ↓
BaseService 处理分页逻辑
    ↓
返回 PageResult 对象
    ↓
Controller 返回统一响应
```

## 功能特性

### 1. 自动配置

- **零配置**：引入依赖后自动生效
- **插件自动注册**：分页、乐观锁、防止全表操作等插件自动注册
- **配置灵活**：支持通过配置文件自定义插件行为

### 2. 动态数据源

- **多数据源支持**：基于 dynamic-datasource-spring-boot-starter
- **数据源切换**：通过 DataSourceContext 工具类切换数据源
- **事务支持**：支持多数据源事务管理
- **执行环境**：提供 execute 方法在指定数据源执行操作

### 3. 基础实体

- **通用字段**：id、createTime、updateTime、deleted、creator、updater
- **自动填充**：创建时间、更新时间、创建者、更新者自动填充
- **逻辑删除**：内置逻辑删除支持
- **序列化支持**：实现 Serializable 接口

### 4. 统一分页

- **统一分页参数**：PageParam 封装分页参数
- **统一分页结果**：PageResult 统一分页响应格式
- **分页插件**：内置分页插件配置
- **分页参数验证**：自动验证分页参数

### 5. 基础 Service

- **继承 IService**：继承 MyBatis-Plus 的 IService，提供完整的 CRUD 操作
- **批量操作**：提供高性能的批量插入和批量插入或更新
- **统一分页**：提供 pageResult 方法返回统一分页结果
- **扩展性**：支持自定义 Service 接口和实现

### 6. 自动填充

- **时间字段**：createTime、updateTime 自动填充
- **操作人字段**：creator、updater 自动填充
- **可自定义**：支持实现 MetaObjectHandler 接口自定义填充逻辑

### 7. 代码生成

- **快速生成**：基于 MyBatis-Plus Generator 实现
- **模板自定义**：支持自定义代码生成模板
- **配置灵活**：支持配置生成策略、包路径等

### 8. 安全特性

- **防止全表操作**：内置 BlockAttackInnerInterceptor
- **乐观锁**：内置 OptimisticLockerInnerInterceptor
- **参数验证**：分页参数自动验证

## 集成使用方法

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.hnhegui.market</groupId>
    <artifactId>hc-mybatis-plus-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- MySQL 驱动（根据实际数据库选择） -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加基础配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

# MyBatis-Plus 配置
hc:
  mybatis-plus:
    enabled: true                    # 是否启用（默认 true）
    page:
      enabled: true                  # 是否启用分页插件（默认 true）
      max-limit: 1000                # 最大分页限制
      overflow: true                 # 是否溢出分页（默认 true）
    dynamic-data-source:
      enabled: false                 # 是否启用动态数据源（默认 false）
```

### 3. 动态数据源配置

如果需要使用动态数据源，配置如下：

```yaml
spring:
  datasource:
    dynamic:
      primary: master                # 默认数据源
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/master?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave:
          url: jdbc:mysql://localhost:3306/slave?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver

hc:
  mybatis-plus:
    dynamic-data-source:
      enabled: true                 # 启用动态数据源
```

### 4. 创建实体类

继承 BaseEntity 创建实体类：

```java
import com.hc.framework.mybatis.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String email;
    private Integer status;
    private String nickname;
}
```

### 5. 创建 Mapper 接口

```java
import com.hc.framework.mybatis.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 自定义查询方法
}
```

### 6. 创建 Service 接口和实现

#### 方式一：使用 BaseService

```java
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.BaseService;

public interface UserService extends BaseService<User> {
    // 自定义服务方法
}

import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.BaseServiceImpl;
import com.hc.framework.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements UserService {
    // 实现自定义服务方法
}
```

#### 方式二：使用默认 IService

```java
import com.hc.framework.mybatis.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    // 自定义服务方法
}

import com.hc.framework.mybatis.entity.User;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hc.framework.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 实现自定义服务方法
}
```

### 7. 使用统一分页

#### Controller 层

```java
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.model.PageParam;
import com.hc.framework.mybatis.model.PageResult;
import com.hc.framework.mybatis.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public PageResult<User> page(PageParam pageParam) {
        return userService.pageResult(pageParam);
    }
}
```

#### 前端请求

```http
GET /api/user/page?page=1&size=10&sort=createTime&order=desc
```

#### 响应结果

```json
{
  "list": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "status": 1,
      "nickname": "管理员",
      "createTime": "2026-04-07T10:00:00",
      "updateTime": "2026-04-07T10:00:00",
      "creator": "1",
      "updater": "1",
      "deleted": 0
    }
  ],
  "total": 100,
  "page": 1,
  "size": 10,
  "totalPages": 10
}
```

### 8. 使用动态数据源

#### 切换数据源

```java
import com.hc.framework.mybatis.config.DynamicDataSourceConfig;
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class DataSyncService {

    private final UserService userService;

    public DataSyncService(UserService userService) {
        this.userService = userService;
    }

    public void syncData() {
        // 从主库查询数据
        DynamicDataSourceConfig.DataSourceContext.switchTo("master");
        User user = userService.getById(1L);
        
        // 切换到从库插入数据
        DynamicDataSourceConfig.DataSourceContext.switchTo("slave");
        userService.save(user);
        
        // 清除数据源上下文
        DynamicDataSourceConfig.DataSourceContext.clear();
    }

    public void syncDataWithExecute() {
        // 使用 execute 方法自动管理数据源切换
        DynamicDataSourceConfig.DataSourceContext.execute("master", () -> {
            User user = userService.getById(1L);
            
            DynamicDataSourceConfig.DataSourceContext.execute("slave", () -> {
                userService.save(user);
            });
        });
    }
}
```

### 9. 使用批量操作

```java
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatchService {

    private final UserService userService;

    public BatchService(UserService userService) {
        this.userService = userService;
    }

    public void batchInsert() {
        List<User> userList = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setPassword("password" + i);
            user.setEmail("user" + i + "@example.com");
            user.setStatus(1);
            user.setNickname("用户" + i);
            userList.add(user);
        }
        
        // 批量插入
        userService.insertBatch(userList);
    }

    public void batchInsertOrUpdate() {
        List<User> userList = new ArrayList<>();
        
        // 已存在的用户（更新）
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("updatedUser");
        userList.add(existingUser);
        
        // 新用户（插入）
        User newUser = new User();
        newUser.setUsername("newUser");
        newUser.setPassword("password");
        newUser.setEmail("newuser@example.com");
        newUser.setStatus(1);
        newUser.setNickname("新用户");
        userList.add(newUser);
        
        // 批量插入或更新
        userService.insertOrUpdateBatch(userList);
    }
}
```

### 10. 使用代码生成器

#### 方式一：直接运行 CodeGenerator

```java
import com.hc.framework.mybatis.generator.CodeGenerator;

public class GeneratorTest {
    public static void main(String[] args) {
        CodeGenerator.generate();
    }
}
```

#### 方式二：自定义配置运行

```java
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CustomGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai", "root", "password")
                .globalConfig(builder -> {
                    builder.author("hc")
                            .outputDir("D:\\work\\hc-spring-cloud-framework\\hc-spring-cloud-framework\\hc-mybatis-plus-spring-boot-starter\\src\\main\\java")
                            .enableSwagger()
                            .commentDate("yyyy-MM-dd");
                })
                .packageConfig(builder -> {
                    builder.parent("com.hc.framework.mybatis")
                            .moduleName("user")
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.mapperXml, "D:\\work\\hc-spring-cloud-framework\\hc-spring-cloud-framework\\hc-mybatis-plus-spring-boot-starter\\src\\main\\resources\\mapper"));
                })
                .strategyConfig(builder -> {
                    builder.addInclude("user")
                            .addTablePrefix("t_")
                            .entityBuilder()
                            .superClass(com.hc.framework.mybatis.entity.BaseEntity.class)
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .controllerBuilder()
                            .enableRestStyle()
                            .serviceBuilder()
                            .superServiceClass(com.hc.framework.mybatis.service.BaseService.class)
                            .superServiceImplClass(com.hc.framework.mybatis.service.BaseServiceImpl.class);
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
```

## 配置说明

### 完整配置示例

```yaml
hc:
  mybatis-plus:
    enabled: true                    # 是否启用
    page:
      enabled: true                  # 是否启用分页插件
      max-limit: 1000                # 最大分页限制
      overflow: true                 # 是否溢出分页
    dynamic-data-source:
      enabled: false                 # 是否启用动态数据源

# 动态数据源配置（当 dynamic-data-source.enabled 为 true 时）
spring:
  datasource:
    dynamic:
      primary: master                # 默认数据源
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/master?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave:
          url: jdbc:mysql://localhost:3306/slave?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver

# MyBatis 基础配置
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.hc.framework.mybatis.entity
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `hc.mybatis-plus.enabled` | 是否启用 MyBatis-Plus 自动配置 | `true` |
| `hc.mybatis-plus.page.enabled` | 是否启用分页插件 | `true` |
| `hc.mybatis-plus.page.max-limit` | 最大分页限制 | `1000` |
| `hc.mybatis-plus.page.overflow` | 是否溢出分页 | `true` |
| `hc.mybatis-plus.dynamic-data-source.enabled` | 是否启用动态数据源 | `false` |
| `spring.datasource.dynamic.primary` | 默认数据源名称 | `master` |
| `spring.datasource.dynamic.datasource` | 数据源配置 | - |

## 高级用法

### 1. 自定义自动填充处理器

```java
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 填充创建者
        this.strictInsertFill(metaObject, "creator", String.class, getCurrentUserId());
        // 填充更新者
        this.strictInsertFill(metaObject, "updater", String.class, getCurrentUserId());
        // 填充删除标志
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 填充更新者
        this.strictUpdateFill(metaObject, "updater", String.class, getCurrentUserId());
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        // 从上下文获取当前用户ID，这里简化处理
        return "1";
    }
}
```

### 2. 自定义分页查询

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.model.PageParam;
import com.hc.framework.mybatis.model.PageResult;
import com.hc.framework.mybatis.service.BaseServiceImpl;
import com.hc.framework.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> {

    /**
     * 自定义条件分页查询
     */
    public PageResult<User> pageByCondition(PageParam pageParam, String username, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        // 构建分页参数
        IPage<User> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        
        // 执行分页查询
        page = baseMapper.selectPage(page, wrapper);
        
        // 构建分页结果
        return PageResult.build(page);
    }
}
```

### 3. 多数据源事务管理

```java
import com.hc.framework.mybatis.config.DynamicDataSourceConfig;
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MultiDataSourceService {

    private final UserService userService;

    public MultiDataSourceService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 多数据源事务
     */
    @Transactional
    public void multiDataSourceTransaction() {
        try {
            // 操作主库
            DynamicDataSourceConfig.DataSourceContext.switchTo("master");
            User user = new User();
            user.setUsername("test");
            user.setPassword("password");
            userService.save(user);
            
            // 操作从库
            DynamicDataSourceConfig.DataSourceContext.switchTo("slave");
            User slaveUser = new User();
            slaveUser.setUsername("test_slave");
            slaveUser.setPassword("password");
            userService.save(slaveUser);
            
            // 模拟异常
            if (true) {
                throw new RuntimeException("模拟异常");
            }
        } finally {
            // 清除数据源上下文
            DynamicDataSourceConfig.DataSourceContext.clear();
        }
    }
}
```

### 4. 自定义代码生成模板

1. **创建自定义模板文件**：
   - 在 `resources/templates` 目录下创建自定义模板
   - 例如：`entity.java.ftl`、`service.java.ftl` 等

2. **使用自定义模板**：

```java
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CustomGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai", "root", "password")
                .globalConfig(builder -> {
                    builder.author("hc")
                            .outputDir("D:\\work\\hc-spring-cloud-framework\\hc-spring-cloud-framework\\hc-mybatis-plus-spring-boot-starter\\src\\main\\java");
                })
                .packageConfig(builder -> {
                    builder.parent("com.hc.framework.mybatis")
                            .moduleName("user");
                })
                .strategyConfig(builder -> {
                    builder.addInclude("user");
                })
                .templateEngine(new FreemarkerTemplateEngine() {
                    @Override
                    protected String getTemplatePath(String filePath) {
                        // 自定义模板路径
                        return "templates/" + filePath;
                    }
                })
                .execute();
    }
}
```

### 5. 性能优化

#### 5.1 批量操作优化

```java
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceService {

    private final UserService userService;

    public PerformanceService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 批量插入优化
     */
    public void batchInsertOptimization() {
        List<User> userList = new ArrayList<>();
        
        // 准备数据
        for (int i = 0; i < 1000; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setPassword("password" + i);
            user.setEmail("user" + i + "@example.com");
            user.setStatus(1);
            user.setNickname("用户" + i);
            userList.add(user);
        }
        
        // 分批插入，每批 100 条
        int batchSize = 100;
        for (int i = 0; i < userList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userList.size());
            List<User> batchList = userList.subList(i, end);
            userService.insertBatch(batchList);
        }
    }
}
```

#### 5.2 分页查询优化

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hc.framework.mybatis.entity.User;
import com.hc.framework.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class PaginationOptimizationService {

    private final UserMapper userMapper;

    public PaginationOptimizationService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 分页查询优化
     */
    public IPage<User> optimizedPageQuery(int page, int size, String username) {
        // 构建查询条件，只查询必要字段
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(User::getId, User::getUsername, User::getEmail, User::getStatus)
                .like(username != null, User::getUsername, username)
                .orderByDesc(User::getCreateTime);
        
        // 构建分页参数
        IPage<User> pageParam = new Page<>(page, size);
        
        // 执行分页查询
        return userMapper.selectPage(pageParam, wrapper);
    }
}
```

## 常见问题

### 1. 如何禁用 MyBatis-Plus 自动配置？

在 `application.yml` 中设置：

```yaml
hc:
  mybatis-plus:
    enabled: false
```

### 2. 如何自定义分页参数？

```java
import com.hc.framework.mybatis.model.PageParam;

public class CustomPageParam extends PageParam {
    private String keyword;
    private Integer status;
    
    // getters and setters
}
```

### 3. 如何处理动态数据源切换后的事务？

使用 `@Transactional` 注解，并在方法结束时清除数据源上下文：

```java
@Transactional
public void multiDataSourceOperation() {
    try {
        // 切换数据源并操作
        DynamicDataSourceConfig.DataSourceContext.switchTo("master");
        // 业务操作
    } finally {
        DynamicDataSourceConfig.DataSourceContext.clear();
    }
}
```

### 4. 如何自定义自动填充的用户信息？

实现自定义的 `MetaObjectHandler`：

```java
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        // 从上下文获取当前用户信息
        String currentUserId = getCurrentUserIdFromContext();
        this.strictInsertFill(metaObject, "creator", String.class, currentUserId);
        this.strictInsertFill(metaObject, "updater", String.class, currentUserId);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        String currentUserId = getCurrentUserIdFromContext();
        this.strictUpdateFill(metaObject, "updater", String.class, currentUserId);
    }
    
    private String getCurrentUserIdFromContext() {
        // 从 ThreadLocal 或 SecurityContext 中获取
        return "1";
    }
}
```

### 5. 如何使用代码生成器生成指定表的代码？

```java
import com.hc.framework.mybatis.generator.CodeGenerator;

public class GeneratorTest {
    public static void main(String[] args) {
        // 生成指定表的代码
        CodeGenerator.generate("user", "role", "permission");
    }
}
```

## 最佳实践

### 1. 实体类设计

- 所有实体类继承 BaseEntity
- 使用 @TableName 注解指定表名
- 使用 @TableField 注解指定字段映射
- 合理使用 @TableLogic 注解实现逻辑删除

### 2. Mapper 接口设计

- 继承 BaseMapper
- 自定义方法使用 @Select、@Insert 等注解
- 复杂查询使用 XML 映射文件

### 3. Service 层设计

- 优先使用 BaseService 接口
- 自定义方法添加业务逻辑
- 批量操作使用 BaseService 提供的批量方法
- 事务管理使用 @Transactional 注解

### 4. 控制器设计

- 使用 PageParam 接收分页参数
- 返回 PageResult 统一分页结果
- 业务异常使用 BusinessException
- 参数校验使用 @Valid 注解

### 5. 动态数据源使用

- 合理规划数据源配置
- 使用 execute 方法管理数据源切换
- 注意事务边界和数据源切换的顺序
- 及时清除数据源上下文

### 6. 性能优化

- 批量操作分批处理
- 分页查询只查询必要字段
- 使用索引优化查询
- 合理使用缓存

## 技术支持

- MyBatis-Plus 官方文档：https://baomidou.com/
- 动态数据源官方文档：https://www.baomidou.com/pages/91a6c0/
- 项目源码：[hc-spring-cloud-framework](https://github.com/your-repo/hc-spring-cloud-framework)
- 问题反馈：[Issues](https://github.com/your-repo/hc-spring-cloud-framework/issues)

## 版本历史

- **1.0.0**：初始版本，提供自动配置、动态数据源、统一分页、自动填充、乐观锁等功能
