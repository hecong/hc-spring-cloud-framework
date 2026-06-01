package com.hc.framework.logging.spi;

/**
 * 用户ID解析器 SPI
 *
 * <p>业务项目实现此接口并提供当前登录用户ID，以启用 {@link com.hc.framework.logging.annotation.RateLimiter}
 * 的 USER 维度限流功能。</p>
 *
 * <p>默认实现返回 null，表示未登录用户（视为 anonymous）。</p>
 *
 * <pre>{@code
 * @Component
 * public class MyUserIdResolver implements UserIdResolver {
 *     public String getCurrentUserId() {
 *         return StpUtil.getLoginIdAsString();
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface UserIdResolver {

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，返回 null 时视为匿名用户
     */
    String getCurrentUserId();
}
