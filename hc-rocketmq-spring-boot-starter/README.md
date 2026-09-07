# hc-rocketmq-spring-boot-starter

## 模块简介

基于 RocketMQ 5.x gRPC 客户端的消息队列 Starter，提供消息发送、消费、事务消息和幂等消费功能。

## 功能特性

- **消息发送**：`RocketMqSender` 统一发送封装（同步、异步、批量、顺序、延迟、单向、事务）
- **消息消费**：`BaseMqConsumer<T>` 消费者基类，泛型自动解析，只需实现 `doConsume(T data)` 即可
- **事务消息 Lambda API**：`sendTransaction(... Consumer<Transaction>)` 内置 commit/rollback
- **事务回查泛型化**：`BaseTransactionChecker<T>` 自动反序列化业务 DTO，不再需要手动 `from(msg)`
- **多 Producer 配置简化**：`RocketMQBaseConfig` 提供 `buildTransactionProducer()` / `buildTransactionTemplate()` helper，每事务 5 行
- **端点自动注入**：push consumer 的 `endpoints` 自动复用 `rocketmq.producer.endpoints`
- **幂等消费**：基于 Redis 的原子标记，业务失败时自动清除标记允许重试
- **TraceId 传递**：自动在 MQ 消息中携带 TraceId（需要 hc-logging，缺失时降级为 MDC）

## 快速开始

```xml
<dependency>
    <groupId>com.hc.framework</groupId>
    <artifactId>hc-rocketmq-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## 使用教程

### 一、发送消息

注入 `RocketMqSender` 即可发送各类消息：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RocketMqSender rocketMqSender;

    // 1. 同步发送
    public void createOrder(OrderDTO order) {
        SendReceipt receipt = rocketMqSender.send("OrderTopic", "create", order);
        log.info("消息发送成功, msgId={}", receipt.getMessageId());
    }

    // 2. 异步发送（不等待结果）
    public void notifyUser(UserNotifyDTO notify) {
        rocketMqSender.sendAsync("NotifyTopic", "sms", notify);
    }

    // 3. 延迟消息（30 分钟后投递）
    public void delayCheck(Long orderId) {
        rocketMqSender.sendDelay("OrderTopic", "timeout", orderId,
                30, TimeUnit.MINUTES);
    }

    // 4. 顺序消息（相同 messageGroup 内保证顺序）
    public void updateInventory(InventoryDTO dto) {
        rocketMqSender.sendOrderly("InventoryTopic", "update", dto,
                dto.getWarehouseId());  // 同仓库的消息串行处理
    }

    // 5. 事务消息 — Lambda API（框架自动 commit/rollback）
    public void payOrder(OrderDTO order) {
        rocketMqSender.sendTransaction("OrderTopic", "paid", order,
                tx -> transactionTemplate.executeWithoutResult(status -> {
                    orderMapper.updateStatus(order.getOrderNo(), PAID);
                    payMapper.insert(buildPayRecord(order));
                }));
    }

    // 6. 事务消息 — 多场景指定 Template
    public void cancelOrder(OrderDTO order) {
        rocketMqSender.sendTransaction("orderCancelTransTemplate",
                "OrderTopic", "cancelled", order,
                tx -> transactionTemplate.executeWithoutResult(status -> {
                    orderMapper.updateStatus(order.getOrderNo(), CANCELLED);
                }));
    }
}
```

### 二、消费消息

继承 `BaseMqConsumer<T>`，只实现 `doConsume(T data)`。泛型类型自动解析，**无需重写 `getDataType()`**：

```java
@Component
@RocketMQMessageListener(
    topic = "OrderTopic",
    tag = "create",
    consumerGroup = "order-create-group"
    // endpoints 会自动注入 rocketmq.producer.endpoints，无需手动指定
)
public class OrderCreateConsumer extends BaseMqConsumer<OrderDTO> {

    @Override
    protected void doConsume(OrderDTO order) {
        // 处理订单创建逻辑
        log.info("收到订单: orderId={}, amount={}", order.getId(), order.getAmount());
    }
}
```

**消息格式**：发送的消息会被自动包装为 `BaseMqMessage`：

```json
{
    "msgId": "uuid-xxx",
    "traceId": "trace-xxx",
    "tenantId": "t1",
    "timestamp": 1717286400000,
    "data": { ... }   // 业务数据
}
```

### 三、事务消息

#### 单 Checker 场景（默认 Template）

```java
// 发送 — Lambda API
rocketMqSender.sendTransaction("OrderTopic", "paid", order,
        tx -> orderService.doPay(order));

// Checker — 泛型自动反序列化
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQClientTemplate")
public class OrderPayChecker extends BaseTransactionChecker<OrderDTO> {
    @Override
    protected boolean doCheckTransaction(OrderDTO order) {
        return orderService.isOrderPaid(order.getOrderNo());
    }
}
```

#### 多 Checker 场景（订单创建 + 订单支付）

当多个事务场景并存时，**继承 `RocketMQBaseConfig`** 并使用 helper 方法，每事务仅需 5 行：

**Step 0：配置类（A-lite 简化版）**

```java
@Configuration
public class RocketMQTransactionConfig extends RocketMQBaseConfig {

    // ========== 订单创建事务 ==========
    @Bean(name = "orderCreateTransProducer", destroyMethod = "close")
    public Producer orderCreateTransProducer(ClientServiceProvider provider,
                                             OrderCreateChecker checker,
                                             RocketMQProperties props) throws ClientException {
        return buildTransactionProducer(provider, checker, props, "OrderTopic");
    }

    @Bean
    public RocketMQClientTemplate orderCreateTransTemplate(
            @Qualifier("orderCreateTransProducer") Producer producer) {
        return buildTransactionTemplate(producer);
    }

    // ========== 订单支付事务 ==========
    @Bean(name = "orderPayTransProducer", destroyMethod = "close")
    public Producer orderPayTransProducer(ClientServiceProvider provider,
                                          OrderPayChecker checker,
                                          RocketMQProperties props) throws ClientException {
        return buildTransactionProducer(provider, checker, props, "OrderTopic");
    }

    @Bean
    public RocketMQClientTemplate orderPayTransTemplate(
            @Qualifier("orderPayTransProducer") Producer producer) {
        return buildTransactionTemplate(producer);
    }
}
```

> `buildTransactionProducer()` / `buildTransactionTemplate()` 由 `RocketMQBaseConfig` 提供，封装了 Producer 构建和 Template 绑定逻辑。

**Step 1：发送时指定 Template Bean 名 + Lambda 事务**

```java
rocketMqSender.sendTransaction("orderCreateTransTemplate", "OrderTopic", "create", order,
        tx -> transactionTemplate.executeWithoutResult(s -> orderService.create(order)));

rocketMqSender.sendTransaction("orderPayTransTemplate", "OrderTopic", "paid", order,
        tx -> transactionTemplate.executeWithoutResult(s -> orderService.pay(order)));
```

**Step 2：Checker 声明 rocketMQTemplateBeanName + 泛型**

```java
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "orderCreateTransTemplate")
public class OrderCreateChecker extends BaseTransactionChecker<OrderDTO> {
    @Override
    protected boolean doCheckTransaction(OrderDTO order) {
        return orderService.isOrderCreated(order.getOrderNo());
    }
}

@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "orderPayTransTemplate")
public class OrderPayChecker extends BaseTransactionChecker<OrderDTO> {
    @Override
    protected boolean doCheckTransaction(OrderDTO order) {
        return orderService.isOrderPaid(order.getOrderNo());
    }
}
```

> **配对关系**：`sendTransaction("xxx", ...)` 的 `xxx` = `@RocketMQTransactionListener(rocketMQTemplateBeanName = "xxx")`。
> 如果 Template 名写错，发送时会抛出 `IllegalArgumentException` 并列出所有可用 Template。

#### 事务消息注意事项

1. **推荐使用 Lambda API**（`sendTransaction(... Consumer<Transaction>)`），框架自动处理 commit/rollback。Lambda 正常返回 → commit，抛异常 → rollback
2. 如果 `commit()` 之前进程崩溃，RocketMQ 会回调 `Checker.check()` 确认状态
3. `Checker.check()` 返回 `true` = 提交消息，`false` = 回滚，抛异常 = UNKNOWN（稍后重试）

### 四、幂等消费

#### 原理

框架提供的是**消息级幂等**（基于 msgId），引入 `hc-redis-spring-boot-starter` 后自动生效。

但消息幂等≠业务幂等。消息重试时可能**上次执行了一半**（部分 DB 写入已提交），因为 Redis 标记在异常时会被清除：

```
首次消费 msgId=xxx:
  tryMarkConsumed → SETNX 成功
  doConsume {
      扣库存  → DB 写入成功 ✅
      发短信  → 抛异常！❌
  }
  catch → remove(msgId) → 标记清除
  return FAILURE → RocketMQ 重试

重试 msgId=xxx:
  tryMarkConsumed → SETNX 成功（上次清除了）
  doConsume {
      扣库存  → 又扣了一次！🔴 重复扣减
  }
```

#### 正确做法：框架标记 + 业务去重

```java
@Override
protected void doConsume(OrderDTO order) {
    // 1. 业务幂等：用业务唯一键判断（如订单号、流水号）
    if (orderService.alreadyProcessed(order.getOrderNo())) {
        log.warn("订单已处理，跳过重试: {}", order.getOrderNo());
        return;
    }

    // 2. 执行业务（利用 DB 唯一索引兜底）
    orderService.createOrder(order);  // INSERT ... ON DUPLICATE KEY / 唯一约束
}
```

**两层防护**：
| 层级 | 机制 | 防什么 |
|---|---|---|
| msgId 幂等（框架） | `tryMarkConsumed` → 原子标记 | 同一条 MQ 消息被投递两次 |
| 业务键幂等（业务） | 唯一索引 / 存在性检查 | 同一条业务数据被处理两次 |

**注意**：不引入 Redis 时，msgId 幂等自动降级为放行（不进行去重），此时业务幂等是唯一防线。

---

## 配置说明

### 框架配置

```yaml
hc:
  rocketmq:
    enabled: true           # 是否启用框架自动配置（默认 true）
    auto-endpoints: true    # 是否自动为 push consumer 注入 endpoints（默认 true）
```

### RocketMQ 5.x 客户端配置

```yaml
rocketmq:
  producer:
    endpoints: localhost:8081         # gRPC Proxy 地址（生产者 + push consumer 共用）
    request-timeout: 3                # 请求超时（秒）
    max-attempts: 3                   # 最大重试次数
  simple-consumer:
    endpoints: ${rocketmq.producer.endpoints}  # consumer endpoints，通常复用 producer 的
    consumer-group: test-group
    await-duration: 30                # 消费等待时长（秒）
```

> **端点自动注入**：对于 push consumer（`@RocketMQMessageListener`），如果未指定 `endpoints`，框架会自动注入 `rocketmq.producer.endpoints` 的值。可通过 `hc.rocketmq.auto-endpoints=false` 关闭此特性。

### Redis 幂等配置（可选）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

> 幂等标记默认 24 小时过期，由 RocketMQ 重试窗口决定。

### TraceId 传递（可选）

引入 `hc-logging-spring-boot-starter` 后，消息的 `traceId` 会自动设置到 MDC，实现全链路追踪。不引入时降级为直接操作 MDC，不影响消息收发。

---

## 本地运行示例

示例代码位于本模块 **`src/test/java/com/hc/framework/rocketmq/example`** 包（仅随 test 编译，不会进入发布的 jar，也不会被引入方业务工程扫描加载）：

| 示例类 | 演示能力 | 关键 Topic / Tag |
|---|---|---|
| `MqTestController` | 发送类 REST 入口：同步 `/normal`、异步 `/async`、单向 `/oneway`、批量 `/batch`、延迟 `/delay`、顺序 `/orderly` | `TEST_NORMAL_TOPIC` / `TEST_ORDER_TOPIC` / `TEST_BATCH_TOPIC`，Tag 统一 `TEST_TAG` |
| `NormalMessageConsumer` | 普通 push 消费：继承 `BaseMqConsumer<T>`，只需实现 `doConsume(T)`，泛型自动反序列化 | 订阅上述发送 Topic |
| `TransactionMessageChecker` | 事务消息回查：继承 `BaseTransactionChecker<T>`，泛型自动反序列化，无需手动 `from(msg)` | 事务场景使用自有 Topic |
| `TestMessageDTO` | 示例消息体 | - |

### 运行前提

1. **基础设施**：一套可访问的 RocketMQ 5.x 环境。本 starter 基于 gRPC 客户端，需提供 **Proxy 端点**（默认 gRPC 端口 `8081`），可用本地 Docker 或官方 quickstart 启动 NameServer + Broker（Proxy）；远程环境则替换为实际端点地址。
2. **关键配置**（test resources 的 `application.yml`）：

```yaml
rocketmq:
  producer:
    endpoints: localhost:8081     # gRPC Proxy 端点（生产者 + push consumer 共用）
    request-timeout: 3
    max-attempts: 3
  simple-consumer:
    endpoints: ${rocketmq.producer.endpoints}
    consumer-group: example-group  # 示例消费组
    await-duration: 30
```

> 示例消息使用的 Topic（`TEST_NORMAL_TOPIC` 等）与消费组需在 Broker 侧已创建或允许自动创建。

### 运行步骤

示例类属于 `src/test` 参考代码，不会自动被执行，需要宿主 Spring Boot 应用上下文来装配。推荐两种方式：

**方式一（推荐，复制到业务工程验证）**

1. 将 `example` 包下的类复制到业务工程的 `src/test/java`（或直接复制代码到业务类）；
2. 按「配置说明」配置 endpoints 与消费组；
3. 用 `@SpringBootTest` 测试类（或直接启动业务应用）加载上下文，观察 `NormalMessageConsumer` 的消费日志；如需 REST 入口触发发送，在业务工程中引入 `spring-boot-starter-web` 后访问 `/mq/test/normal?content=hello` 等接口。

**方式二（本模块内最小引导）**

```java
@SpringBootApplication
@Import(NormalMessageConsumer.class)   // 引入示例消费 Bean（按需增删）
public class LocalMqBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(LocalMqBootstrap.class, args);
    }
}
```

在本模块 test 集内创建上述引导类后运行（注意需先启动 RocketMQ Proxy），即可在日志中看到示例消费者的收发效果。

> **示例边界约定（评审检查项）**：所有示例 / 演示类**只能放在 `src/test`**（如 `example` 包），**禁止写入 `src/main`**，否则业务工程引入本 starter 后会错误扫描并加载示例 Bean 与 REST 端点（如 `MqTestController` 被误暴露）。模块 `package` 产物已核验不含 `com/hc/framework/rocketmq/example/` 类；`src/main` 不允许出现对 `example` 包的引用。

---

## 依赖说明

| 依赖 | 是否必需 | 用途 |
|---|---|---|
| Java 21 | 必需 | 运行环境 |
| RocketMQ 5.x gRPC Client | 必需 | 消息队列核心 |
| hc-common-spring-boot-starter | 必需 | JSON 序列化 |
| spring-boot-starter | 必需 | Spring 容器 |
| hc-redis-spring-boot-starter | 可选 | 幂等消费（不引入时自动降级） |
| hc-logging-spring-boot-starter | 可选 | TraceId 全链路传递（不引入时降级为 MDC） |
