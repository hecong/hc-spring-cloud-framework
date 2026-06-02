package com.hc.framework.satoken.config;

import cn.dev33.satoken.stp.StpUtil;
import com.hc.framework.common.spi.UserIdProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 用户ID提供者自动配置
 *
 * <p>当项目中同时引入了 hc-mybatis-plus-spring-boot-starter 和
 * hc-satoken-spring-boot-starter 时，自动创建基于 {@link StpUtil} 的
 * {@link UserIdProvider} 实现，实现零配置自动填充 creator/updater 字段。</p>
 *
 * <p>行为：</p>
 * <ul>
 *   <li>已登录：返回 {@code StpUtil.getLoginIdAsString()}</li>
 *   <li>未登录：返回空字符串（安全写入 NOT NULL 列）</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConditionalOnClass(StpUtil.class)
public class SaTokenUserIdProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserIdProvider.class)
    public UserIdProvider saTokenUserIdProvider() {
        log.info("Sa-Token 用户ID提供者已启用 — creator/updater 将自动从登录态获取");
        return () -> {
            try {
                return StpUtil.getLoginIdAsString();
            } catch (Exception e) {
                return "";
            }
        };
    }
}
