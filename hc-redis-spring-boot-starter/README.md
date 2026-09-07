# HC Redis Spring Boot Starter

基于 Spring Boot 的 Redis 缓存和分布式锁 Starter，提供缓存管理、分布式锁、防重复提交等功能。

## 升级与发布顺序（请先阅读）

> 本版本包含 **破坏性变更**：多态反序列化白名单（安全加固）。按以下顺序操作可避免升级后缓存读取失败：

1. **业务先配置白名单**：在应用中配置 `hc.redis.allowed-packages`，将自身实体 / DTO / 缓存对象所在包前缀全部列入
   （该配置在旧框架版本中会被忽略，不影响旧版运行，可先行发布）。
2. **再升级框架版本**：升级后多态反序列化仅放行「默认白名单 ∪ 已配置白名单」中的类型；
   默认白名单未覆盖的业务包缓存对象将被拒绝并抛出反序列化异常。
3. **观察与清理**：升级后关注反序列化异常日志（会明确指出被拒类型），对白名单外的存量缓存 key 执行清理或刷新一次即可恢复。
4. **代码适配（按需）**：本版本同时收口了 Redis key 前缀常量与自增序列取号方式、调整了锁内异常透传语义，
   若代码中手动内联过 `seq:` / `seq:global:` 等字面量或依赖锁异常包装行为，请按文末「注意事项」适配。

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

### 多态反序列化白名单（业务包必配）

```yaml
hc:
  redis:
    allowed-packages:
      - com.hnhegui.order.domain.
      - com.hnhegui.market.entity.
```

- 与默认白名单（`com.hc.framework.`、`com.hnhegui.`、`java.util.`、`java.lang.`、`java.time.`）**取并集，仅追加不覆盖**；
  前缀缺省结尾 `.` 会自动补全。
- 业务实体 / DTO 所在包若不在默认白名单内，**必须在本版本升级前先行配置**，否则升级后缓存反序列化会被白名单拒绝并抛异常。
- 新增缓存对象的包前缀建议按下方「Key 命名规范」统一规划，并在本文档中登记。

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

### 1. 序列化（多态白名单）

- 默认使用 JSON 序列化，支持 JDK8+ 日期时间类型；缓存的类需要有无参构造方法
- 缓存值会携带类型元数据（类名等），反序列化仅放行「默认白名单 ∪ `hc.redis.allowed-packages`」中的类型，
  白名单外类型会被拒绝（抛类型校验异常，防止反序列化 RCE）
- 业务包默认已覆盖（`com.hc.framework.` / `com.hnhegui.`）则无需配置；否则请按「配置说明 → 多态反序列化白名单」配置
- 若反序列化因白名单被拒，清理/刷新该缓存 key 一次即可；也请检查业务缓存对象是否遗漏配置其包前缀

### 2. 分布式锁

- 默认使用可重入锁，自动续期；锁的 Key 建议包含业务标识，避免冲突；批量锁使用 Redisson 的 MultiLock
- **异常语义（自本版本起）**：锁内回调抛出的 `RuntimeException`（如业务异常）**原样透传不包装**；
  受检异常包装为 `LOCK_EXECUTION_FAILED`（原始异常作为 cause 保留）；`InterruptedException` 会先恢复线程中断标志再抛 `LOCK_INTERRUPTED`
- 无论成功、异常或中断，锁都会在退出前释放，后续线程可再次获取

### 3. 防重复提交

- 默认过期时间为 1 秒
- SPEL 表达式中可使用 `args`（参数数组）和 `target`（目标对象）
- 建议根据业务场景设置合理的过期时间

### 4. 缓存 Key 命名规范（新增 key 必读）

- **新增前缀统一为：`hc:{模块}:{业务}`**，模块 / 业务使用小写字母与连字符
  （示例：`hc:order:lock:create:123`、`hc:order:cache:detail:456`）
- **禁止新增**无业务语义的裸前缀；新增前缀统一经 `RedisKeyConstants` 常量引用，不再内联字面量
- 存量前缀（`repeat:submit:`、`seq:`、`seq:global:` 及 LockTemplate 的业务 key）为兼容线上数据保留原值，不做强制迁移
- 批量删除 `deleteByPrefix` 基于 **SCAN 游标 + 分批 DELETE**，不会阻塞 Redis 单线程、幂等可重入；
  但前缀匹配量极大时耗时随 key 数量线性增长，请评估调用频率，严禁使用空前缀做全库删除
- 自增序列取号已由单条 Lua 原子完成（INCR + 首次 EXPIRE 2 天），不会残留无过期 key；
  底层取值缺失时返回全 0（如 `0000`），请勿将其当作有效业务号持久化

### 5. 异常处理

分布式锁抛出的 `LockException` 包含错误码：

```java
try {
    lockTemplate.execute("lock:key", action);
} catch (LockException e) {
    if (e.getCode() == LockException.LOCK_BUSY) {
        // 锁被占用
    } else if (e.getCode() == LockException.LOCK_ACQUIRE_FAILED) {
        // 获取锁失败 / 等待获取锁时被中断
    } else if (e.getCode() == LockException.LOCK_INTERRUPTED) {
        // 持锁执行时线程被中断（中断标志已恢复）
    } else if (e.getCode() == LockException.LOCK_EXECUTION_FAILED) {
        // 持锁执行业务逻辑失败（e.getCause() 为原始异常）
    }
}
```

> 注意：锁内回调抛出的 RuntimeException 会原样透传，不会进入本 `catch (LockException)` 分支，
> 请按业务异常（如 `BusinessException`）的既有全局处理逻辑捕获。

## 依赖说明

- Spring Boot 3.x
- Spring Data Redis
- Redisson
- Hutool
