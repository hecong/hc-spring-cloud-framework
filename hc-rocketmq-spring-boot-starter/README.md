# hc-rocketmq-spring-boot-starter

## 模块简介

基于 RocketMQ 5.x gRPC 客户端的消息队列 Starter，提供消息发送、消费、事务消息和幂等消费功能。

## 功能特性

- **消息发送**：`RocketMqSender` 统一发送封装（同步、异步、批量、顺序、延迟、单向、事务）
- **消息消费**：`BaseMqConsumer` 消费者基类，只需实现 `doConsume(T data)` 即可
- **事务消息**：`BaseTransactionChecker` 事务回查基类
- **幂等消费**：基于 Redis 的原子标记，业务失败时自动清除标记允许重试
- **TraceId 传递**：自动在 MQ 消息中携带 TraceId（需要 hc-logging，缺失时降级为 MDC）

## 快速开始

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
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

    // 3. 延迟消息（30秒后投递）
    public void delayCheck(Long orderId) {
        rocketMqSender.sendDelay("OrderTopic", "timeout", orderId,
                Duration.ofSeconds(30));
    }

    // 4. 顺序消息（相同 messageGroup 内保证顺序）
    public void updateInventory(InventoryDTO dto) {
        rocketMqSender.sendOrderly("InventoryTopic", "update", dto,
                dto.getWarehouseId());  // 同仓库的消息串行处理
    }

    // 5. 批量发送（返回成功数）
    public void batchSync(List<OrderDTO> orders) {
        int successCount = rocketMqSender.sendBatch("OrderTopic", "sync", orders);
        if (successCount < orders.size()) {
            log.warn("批量发送部分失败: {}/{}", successCount, orders.size());
        }
    }

    // 6. 事务消息（单场景，使用默认模板）
    public void payOrder(OrderDTO order) {
        TransactionResult result = rocketMqSender.sendTransaction(
                "OrderTopic", "paid", order);
        doTransaction(result, () -> orderService.doPay(order));
    }

    // 6b. 事务消息（多场景，指定模板名。模板名 = Checker 的 rocketMQTemplateBeanName）
    public void createOrder(OrderDTO order) {
        TransactionResult result = rocketMqSender.sendTransaction(
                "orderCreateRocketMQClientTemplate", "OrderTopic", "create", order);
        doTransaction(result, () -> orderService.doCreate(order));
    }

    private void doTransaction(TransactionResult result, Runnable localTx) {
        try {
            localTx.run();
            result.getTransaction().commit();
        } catch (Exception e) {
            result.getTransaction().rollback();
            throw e;
        }
    }
}
```

### 二、消费消息

继承 `BaseMqConsumer<T>`，实现 `doConsume` 和 `getDataType`：

```java
@Component
@RocketMQMessageListener(
    topic = "OrderTopic",
    tag = "create",
    consumerGroup = "order-create-group"
)
public class OrderCreateConsumer extends BaseMqConsumer<OrderDTO> {

    @Override
    protected Class<OrderDTO> getDataType() {
        return OrderDTO.class;
    }

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

只有一个事务场景时，使用默认 Template 即可：

```java
// 发送
rocketMqSender.sendTransaction("OrderTopic", "paid", order);

// Checker
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQClientTemplate")
public class OrderPayChecker extends BaseTransactionChecker {
    @Override
    protected boolean doCheckTransaction(BaseMqMessage msg) {
        return orderService.isOrderPaid(msg);  // 检查支付状态
    }
}
```

#### 多 Checker 场景（如订单创建 + 订单支付）

当多个事务场景并存时，**Template Bean 名 = `@RocketMQTransactionListener` 的 `rocketMQTemplateBeanName`**：

**Step 1：定义多个 Template Bean**

```java
@Configuration
public class MqTemplateConfig {

    @Bean
    public RocketMQClientTemplate orderCreateRocketMQClientTemplate(
            RocketMQProperties properties) {
        return createTemplate(properties, "order-create-producer");
    }

    @Bean
    public RocketMQClientTemplate orderPayRocketMQClientTemplate(
            RocketMQProperties properties) {
        return createTemplate(properties, "order-pay-producer");
    }
}
```

**Step 2：发送时指定 Template 名**

```java
// 订单创建 → 用 orderCreateRocketMQClientTemplate
rocketMqSender.sendTransaction("orderCreateRocketMQClientTemplate",
        "OrderTopic", "create", order);

// 订单支付 → 用 orderPayRocketMQClientTemplate
rocketMqSender.sendTransaction("orderPayRocketMQClientTemplate",
        "OrderTopic", "paid", order);
```

**Step 3：Checker 上配对同名**

```java
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "orderCreateRocketMQClientTemplate")
public class OrderCreateChecker extends BaseTransactionChecker {
    @Override
    protected boolean doCheckTransaction(BaseMqMessage msg) {
        return orderService.isOrderCreated(msg);
    }
}

@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "orderPayRocketMQClientTemplate")
public class OrderPayChecker extends BaseTransactionChecker {
    @Override
    protected boolean doCheckTransaction(BaseMqMessage msg) {
        return orderService.isOrderPaid(msg);
    }
}
```

> **配对关系**：`sendTransaction("xxx", ...)` 的 `xxx` = `@RocketMQTransactionListener(rocketMQTemplateBeanName = "xxx")`。
> 如果 Template 名写错，发送时会抛出 `IllegalArgumentException` 并列出所有可用 Template。

#### 事务消息使用注意事项

1. 发送后必须调用 `result.getTransaction().commit()` 或 `rollback()`
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
    enabled: true      # 是否启用框架自动配置（默认 true）
```

### RocketMQ 5.x 客户端配置

```yaml
rocketmq:
  endpoints: localhost:8081           # gRPC Proxy 地址
  producer:
    topics: OrderTopic;NotifyTopic    # 生产者主题列表
    max-attempts: 3
  simple-consumer:
    topics: OrderTopic:create:order-create-group  # 主题:标签:消费组
    await-duration: 30s
```

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

## 依赖说明

| 依赖 | 是否必需 | 用途 |
|---|---|---|
| Java 17 | 必需 | 运行环境 |
| RocketMQ 5.x gRPC Client | 必需 | 消息队列核心 |
| hc-common-spring-boot-starter | 必需 | JSON 序列化 |
| spring-boot-starter | 必需 | Spring 容器 |
| hc-redis-spring-boot-starter | 可选 | 幂等消费（不引入时自动降级） |
| hc-logging-spring-boot-starter | 可选 | TraceId 全链路传递（不引入时降级为 MDC） |
