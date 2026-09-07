## 1. 示例边界核验（example-src-test）

- [x] 1.1 `mvn -pl hc-rocketmq-spring-boot-starter test-compile` 通过——`example` 包（含 `MqTestController` 等 spring-web 注解类）在现有 test classpath 编译正常，**无需**新增 test scope `spring-boot-starter-web` 依赖（pom 零改动）
- [x] 1.2 `mvn package` 后 `jar tf` 主 jar（`hc-rocketmq-spring-boot-starter-1.0-SNAPSHOT.jar`）检索 `example` 为 none——示例类不进入发布产物
- [x] 1.3 全仓检索 `src/main` 中对 `example` 包引用为空——主代码无示例污染

## 2. 文档

- [x] 2.1 README「本地运行示例」章节：基础设施前提（RocketMQ 5.x Proxy gRPC 8081）、关键配置 `application.yml` 示例、示例类清单表（MqTestController/NormalMessageConsumer/TransactionMessageChecker/TestMessageDTO 与 Topic/Tag）、两种运行步骤（复制到业务工程 `@SpringBootTest` 验证 / 本模块最小 `@SpringBootApplication` 引导）；验证：章节存在、步骤可执行、与真实示例类一致
- [x] 2.2 README 写入示例边界约定（评审检查项）：示例/演示类仅限 `src/test`（example 包），禁止进入 `src/main`，避免引入方错误扫描示例 Bean 与 REST 端点；验证：约定随「本地运行示例」章节发布

## 3. 验证

- [x] 3.1 全量 `mvn verify` **BUILD SUCCESS**（含 rocketmq 模块 test/package/javadoc 阶段）
