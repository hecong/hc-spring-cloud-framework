# HC Redis Spring Boot Starter

基于 Spring Boot 的 Redis 缓存和分布式锁 Starter，提供缓存管理、分布式锁、防重复提交等功能。

## 功能特性

- **Redis 缓存**：支持 JSON 序列化，支持自定义过期时间
- **分布式锁**：基于 Redisson，支持可重入锁、公平锁、读写锁等
- **防重复提交**：基于注解的防重复提交功能，支持 SPEL 表达式
- **缓存工具类**：RedisCacheUtils 提供常用缓存操作

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.hnhegui.market</groupId>
    <artifactId>hc-redis-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
```

### 3. 启用缓存（可选）

```java
@SpringBootApplication
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 配置说明

### 缓存配置

```yaml
hc:
  cache:
    redis-scan-batch-size: 30  # Redis scan 批量大小
```

### Redisson 配置（可选）

```yaml
redisson:
  single-server-config:
    address: "redis://localhost:6379"
    password: 
    database: 0
```

## 使用示例

### 1. 分布式锁

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final LockTemplate lockTemplate;
    
    public void createOrder(OrderDTO dto) {
        String lockKey = "order:create:" + dto.getUserId();
        
        lockTemplate.execute(lockKey, () -> {
            // 业务逻辑
            doCreateOrder(dto);
        });
    }
    
    // 带自定义错误提示
    public void createOrderWithMsg(OrderDTO dto) {
        String lockKey = "order:create:" + dto.getUserId();
        
        lockTemplate.execute(lockKey, "正在处理中，请勿重复提交", () -> {
            doCreateOrder(dto);
        });
    }
    
    // 批量加锁
    public void batchUpdate(List<String> ids) {
        lockTemplate.execute(ids, () -> {
            // 批量处理逻辑
        });
    }
}
```

### 2. 防重复提交

```java
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    
    @PostMapping("/create")
    @RepeatSubmit(expire = 5, message = "请勿重复提交订单")
    public Result<Void> create(@RequestBody OrderDTO dto) {
        // 处理逻辑
        return Result.ok();
    }
    
    // 使用 SPEL 表达式自定义 Key
    @PostMapping("/cancel")
    @RepeatSubmit(key = "#dto.orderId", expire = 3)
    public Result<Void> cancel(@RequestBody OrderDTO dto) {
        // 处理逻辑
        return Result.ok();
    }
    
    // 组合 Key
    @PostMapping("/update")
    @RepeatSubmit(key = "#userId + ':' + #dto.id", expire = 2)
    public Result<Void> update(@RequestHeader("User-Id") String userId, 
                               @RequestBody OrderDTO dto) {
        // 处理逻辑
        return Result.ok();
    }
}
```

### 3. 缓存工具类

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final RedisCacheUtils redisCache;
    
    public User getUser(String userId) {
        // 获取缓存
        User user = redisCache.get("user:" + userId);
        if (user == null) {
            user = loadFromDb(userId);
            // 设置缓存
            redisCache.set("user:" + userId, user, 30, TimeUnit.MINUTES);
        }
        return user;
    }
    
    public void deleteUser(String userId) {
        // 删除缓存
        redisCache.delete("user:" + userId);
        // 批量删除
        redisCache.deleteByPrefix("user:");
    }
    
    // Hash 操作
    public void saveUserConfig(String userId, Map<String, Object> config) {
        redisCache.hPutAll("user:config:" + userId, config);
    }
    
    // List 操作
    public void addToQueue(String task) {
        redisCache.rPush("task:queue", task);
    }
    
    public String popFromQueue() {
        return redisCache.lPop("task:queue");
    }
}
```

### 4. Spring Cache 自定义过期时间

```java
@Service
public class ProductService {
    
    // 默认过期时间
    @Cacheable(value = "product", key = "#id")
    public Product getProduct(Long id) {
        return loadFromDb(id);
    }
    
    // 自定义过期时间：300秒
    @Cacheable(value = "product#300", key = "#id")
    public Product getProductCustomTtl(Long id) {
        return loadFromDb(id);
    }
    
    // 单位支持：d(天)、h(小时)、m(分钟)、s(秒)
    @Cacheable(value = "product#1h", key = "#id")
    public Product getProductOneHour(Long id) {
        return loadFromDb(id);
    }
}
```

## 注意事项

### 1. 序列化

- 默认使用 JSON 序列化，支持 JDK8 日期时间类型
- 缓存的类需要有无参构造方法

### 2. 分布式锁

- 默认使用可重入锁，自动续期
- 锁的 Key 建议包含业务标识，避免冲突
- 批量锁使用 Redisson 的 MultiLock，保证原子性

### 3. 防重复提交

- 默认过期时间为 1 秒
- SPEL 表达式中可使用 `args`（参数数组）和 `target`（目标对象）
- 建议根据业务场景设置合理的过期时间

### 4. 缓存 Key 规范

- 避免使用特殊字符
- 建议格式：`业务:模块:标识`
- 批量删除时慎用 `*` 通配符

### 5. 异常处理

分布式锁抛出的 `LockException` 包含错误码：

```java
try {
    lockTemplate.execute("lock:key", action);
} catch (LockException e) {
    if (e.getCode() == LockException.LOCK_BUSY) {
        // 锁被占用
    } else if (e.getCode() == LockException.LOCK_ACQUIRE_FAILED) {
        // 获取锁失败
    }
}
```

## 依赖说明

- Spring Boot 3.x
- Spring Data Redis
- Redisson
- Hutool
