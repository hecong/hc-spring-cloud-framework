package com.hc.framework.common.spi;

/**
 * 当前用户ID提供者 SPI
 *
 * <p>业务项目实现此接口并注册为 Spring Bean，框架即可自动获取当前操作人。</p>
 * <p>默认实现返回空字符串（安全写入 NOT NULL 列）。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 配合 Sa-Token
 * @Bean
 * public UserIdProvider userIdProvider() {
 *     return () -> {
 *         try { return StpUtil.getLoginIdAsString(); }
 *         catch (Exception e) { return ""; }
 *     };
 * }
 * }</pre>
 *
 * <p>如果同时引入了 hc-satoken-spring-boot-starter，框架会自动创建上述实现，无需手动配置。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface UserIdProvider {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID字符串，未登录时返回空字符串
     */
    String getCurrentUserId();
}
