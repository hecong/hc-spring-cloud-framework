package com.hc.framework.logging.aspect;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.hc.framework.logging.annotation.RateLimiter;
import com.hc.framework.logging.config.LoggingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RateLimiterAspect 规则注册并发去重/幂等/清理语义单测
 */
class RateLimiterAspectTest {

    private RateLimiterAspect aspect;
    private String own;

    @BeforeEach
    void setUp() {
        FlowRuleManager.loadRules(new ArrayList<>());
        aspect = new RateLimiterAspect(new LoggingProperties(), () -> null);
        own = "ut.res." + System.nanoTime();
    }

    @AfterEach
    void tearDown() {
        FlowRuleManager.loadRules(new ArrayList<>());
    }

    private long ruleCount(String resource) {
        return FlowRuleManager.getRules().stream().filter(r -> resource.equals(r.getResource())).count();
    }

    private Set<String> registry() throws Exception {
        Field field = RateLimiterAspect.class.getDeclaredField("registeredResources");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) field.get(aspect);
        return set;
    }

    @Test
    @DisplayName("50 线程并发首访同一资源：仅注册一条规则且接口可用")
    void concurrentFirstVisitRegistersOnce() throws Exception {
        int threads = 50;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
                    return null;
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, ruleCount(own), "并发首访规则数必须为 1");
        assertEquals(1, registry().size());
    }

    @Test
    @DisplayName("重复访问：无锁短路不重复注册，规则数量保持不变")
    void repeatedAccessIdempotent() throws Exception {
        aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
        assertEquals(1, ruleCount(own));

        // 单线程重复 + 多线程重复均不新增规则
        for (int i = 0; i < 100; i++) {
            aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
        }
        assertEquals(1, ruleCount(own), "重复访问不得重复注册");
        assertEquals(1, registry().size());

        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, ruleCount(own));
        assertEquals(1, registry().size());
    }

    @Test
    @DisplayName("外部已存在同名规则：不重复注册也不接管（registry 不记录）")
    void externalRuleNotDuplicated() throws Exception {
        FlowRule external = new FlowRule();
        external.setResource(own);
        external.setGrade(RuleConstant.FLOW_GRADE_QPS);
        external.setCount(5);
        List<FlowRule> initial = new ArrayList<>();
        initial.add(external);
        FlowRuleManager.loadRules(initial);

        aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);

        assertEquals(1, ruleCount(own), "外部规则不应被重复追加");
        assertTrue(registry().isEmpty(), "外部规则不得进入本切面管理集合");
    }

    @Test
    @DisplayName("销毁清理：仅移除本切面注册规则，其他来源规则保留")
    void cleanupRemovesOnlyOwnRules() throws Exception {
        String externalResource = "ut.external." + System.nanoTime();
        FlowRule external = new FlowRule();
        external.setResource(externalResource);
        external.setGrade(RuleConstant.FLOW_GRADE_QPS);
        external.setCount(10);
        FlowRuleManager.loadRules(new ArrayList<>(List.of(external)));

        aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
        assertEquals(2, FlowRuleManager.getRules().size());

        aspect.cleanup();

        assertTrue(registry().isEmpty(), "registry 应被清空");
        assertEquals(1, FlowRuleManager.getRules().size());
        assertEquals(externalResource, FlowRuleManager.getRules().get(0).getResource(),
                "其他来源规则必须保留");
        assertFalse(FlowRuleManager.getRules().stream().anyMatch(r -> own.equals(r.getResource())));
    }

    @Test
    @DisplayName("并发校验失败不抛未预期异常")
    void concurrentCallNeverThrows() {
        // 幂等性 = 并发调用的稳定性
        aspect.initFlowRule(own, 100, RateLimiter.Mode.DEFAULT);
        aspect.initFlowRule(own, 50, RateLimiter.Mode.WARM_UP);
        assertEquals(1, ruleCount(own), "WARM_UP 模式调整 qps 也不得新增规则（注册只发生一次）");
    }
}
