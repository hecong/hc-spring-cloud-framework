package com.hc.framework.satoken.config;

import com.hc.framework.mybatis.handler.DefaultMetaObjectHandler;
import com.hc.framework.satoken.util.SaTokenHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 与 MyBatis-Plus 集成自动配置类
 * <p>
 * 当项目中同时存在 Sa-Token 和 MyBatis-Plus 时，自动注册当前用户ID提供者，
 * 用于 MyBatis-Plus 的自动填充功能（creator、updater 字段）。
 *
 * @author hc
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass({SaTokenHelper.class, DefaultMetaObjectHandler.class})
@ConditionalOnProperty(prefix = "hc.satoken.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenMyBatisPlusAutoConfiguration {

    private final SaTokenHelper saTokenHelper;

    /**
     * 注册当前用户ID提供者
     * <p>
     * 在 Bean 初始化完成后执行，确保 SaTokenHelper 已可用
     */
    @PostConstruct
    public void registerCurrentUserIdSupplier() {
        DefaultMetaObjectHandler.registerCurrentUserIdSupplier(() -> {
            try {
                return saTokenHelper.getCurrentUserIdString();
            } catch (Exception e) {
                // 未登录或获取失败时返回 null
                log.debug("获取当前用户ID失败: {}", e.getMessage());
                return null;
            }
        });
        log.info("Sa-Token 与 MyBatis-Plus 集成已启用，已注册当前用户ID提供者");
    }
}
