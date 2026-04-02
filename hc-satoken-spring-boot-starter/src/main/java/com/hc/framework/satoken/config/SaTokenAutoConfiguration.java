package com.hc.framework.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpInterface;
import com.hc.framework.redis.util.RedisCacheUtils;
import com.hc.framework.satoken.handler.SaPermissionProvider;
import com.hc.framework.satoken.handler.SaTokenAuthLogger;
import com.hc.framework.satoken.handler.SaTokenStpInterfaceImpl;
import com.hc.framework.satoken.scheduler.SaTokenCleanScheduler;
import com.hc.framework.satoken.service.SaJwtTokenService;
import com.hc.framework.satoken.service.SaPermissionCacheService;
import com.hc.framework.satoken.util.SaPasswordEncoder;
import com.hc.framework.satoken.util.SaTokenHelper;
import com.hc.framework.satoken.util.SaTokenParser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sa-Token 自动配置类
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>hc.satoken.enabled: 是否启用 Sa-Token 功能（默认 true）</li>
 *   <li>hc.satoken.token: Token 配置（名称、有效期、风格等）</li>
 *   <li>hc.satoken.cookie: Cookie 配置</li>
 *   <li>hc.satoken.redis: Redis 存储配置</li>
 *   <li>hc.satoken.permission: 权限配置</li>
 *   <li>hc.satoken.jwt: JWT 配置</li>
 *   <li>hc.satoken.sso: SSO 配置</li>
 *   <li>hc.satoken.token-clean: Token 清理配置</li>
 * </ul>
 *
 * <p>本配置类提供以下核心功能：</p>
 * <ul>
 *   <li>Token 存储适配（自动检测 Redis 环境）</li>
 *   <li>Token 过期清理（定时任务）</li>
 *   <li>多 Token 风格支持（UUID/JWT/雪花算法）</li>
 *   <li>权限数据加载接口实现，支持缓存</li>
 *   <li>URL 权限拦截器</li>
 * </ul>
 *
 * <p>开箱即用特性：</p>
 * <ul>
 *   <li>注解权限自动生效：引入 Starter 后直接使用 @SaCheckLogin/@SaCheckRole/@SaCheckPermission</li>
 *   <li>路径匹配权限：配置 url-permissions 即可拦截鉴权</li>
 *   <li>角色/权限缓存：配置 permission.cache-enabled=true 自动缓存</li>
 *   <li>自定义权限处理器：实现 SaPermissionProvider 接口自动注入</li>
 *   <li>Token 存储适配：引入 hc-redis-spring-boot-starter 后自动使用 Redis 存储</li>
 *   <li>Token 过期清理：配置 token-clean.enabled=true 自动清理过期 Token</li>
 *   <li>JWT Token：配置 jwt.enabled=true 或 token.style=jwt 自动切换</li>
 *   <li>SSO 单点登录：配置 sso.enabled=true 快速接入 SSO</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     enabled: true
 *     token:
 *       style: uuid
 *     jwt:
 *       enabled: false
 *       secret: your-jwt-secret
 *     token-clean:
 *       enabled: true
 *       cron: "0 0 3 * * ?"
 *     sso:
 *       enabled: false
 *       server-url: http://sso.example.com
 *     permission:
 *       enabled: true
 *       cache-enabled: true
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SaTokenProperties.class)
@EnableScheduling
@ConditionalOnProperty(prefix = "hc.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({SaTokenWebMvcConfiguration.class, SaSsoAutoConfiguration.class, SaTokenCorsConfiguration.class})
public class SaTokenAutoConfiguration {

    private final SaTokenProperties saTokenProperties;

    /**
     * 构造器注入 SaTokenProperties，确保配置属性已加载
     */
    public SaTokenAutoConfiguration(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    // ==================== Token 存储与清理 ====================

    /**
     * Token 过期清理定时任务
     *
     * <p>定时清理 Redis 中的过期 Token 数据，释放存储空间。</p>
     * <p>需要 Redis 环境支持。</p>
     */
    @Bean
    @ConditionalOnBean(SaTokenDao.class)
    @ConditionalOnProperty(prefix = "hc.satoken.token-clean", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public SaTokenCleanScheduler saTokenCleanScheduler(SaTokenDao saTokenDao) {
        return new SaTokenCleanScheduler(saTokenProperties, saTokenDao);
    }

    // ==================== JWT Token ====================

    /**
     * JWT Token 服务
     *
     * <p>提供 JWT Token 生成、解析、验证等功能。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaJwtTokenService saJwtTokenService() {
        return new SaJwtTokenService(saTokenProperties);
    }

    // ==================== 权限管理 ====================

    /**
     * 权限缓存服务
     *
     * <p>使用 Redis 缓存用户的角色和权限信息，避免重复查询数据库。</p>
     * <p>当 Redis 不可用时，自动降级为直接查询。</p>
     */
    @Bean
    @ConditionalOnBean(RedisCacheUtils.class)
    @ConditionalOnMissingBean
    public SaPermissionCacheService saPermissionCacheService(RedisCacheUtils redisCacheUtils) {
        return new SaPermissionCacheService(redisCacheUtils, saTokenProperties);
    }

    /**
     * 权限数据加载接口实现（支持缓存）
     *
     * <p>优先从缓存获取角色和权限，缓存未命中时从数据源加载。</p>
     */
    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface(SaPermissionProvider permissionProvider,
                                     SaPermissionCacheService cacheService) {
        return new SaTokenStpInterfaceImpl(permissionProvider, cacheService);
    }

    /**
     * 默认权限提供者（空实现）
     *
     * <p>当业务项目没有提供 SaPermissionProvider 实现时，使用此默认实现。</p>
     * <p>返回空的角色和权限列表，适用于仅需要登录认证的场景。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SaPermissionProvider.class)
    public SaPermissionProvider defaultSaPermissionProvider() {
        return new SaPermissionProvider() {
            @Override
            public java.util.List<String> getRoles(Long userId) {
                return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<String> getPermissions(Long userId) {
                return java.util.Collections.emptyList();
            }
        };
    }

    // ==================== 工具类 ====================

    /**
     * Token 操作工具类
     *
     * <p>提供常用的 Token 操作方法，简化业务代码。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenHelper saTokenHelper() {
        return new SaTokenHelper(saTokenProperties);
    }

    /**
     * Token 解析器
     *
     * <p>支持从 Header/Parameter/Cookie 中解析 Token，优先级可配置。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenParser saTokenParser() {
        return new SaTokenParser(saTokenProperties);
    }

    /**
     * 认证日志记录器
     *
     * <p>自动记录登录成功/失败、权限校验失败等认证相关日志。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaTokenAuthLogger saTokenAuthLogger() {
        return new SaTokenAuthLogger(saTokenProperties);
    }

    /**
     * 初始化密码编码器默认算法
     *
     * <p>根据配置设置默认加密算法。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public SaPasswordEncoderInitializer saPasswordEncoderInitializer() {
        String algorithm = saTokenProperties.getPassword().getAlgorithm();
        SaPasswordEncoder.PasswordAlgorithm passwordAlgorithm = parsePasswordAlgorithm(algorithm);
        SaPasswordEncoder.setDefaultAlgorithm(passwordAlgorithm);
        return new SaPasswordEncoderInitializer(passwordAlgorithm);
    }

    /**
     * 解析密码算法
     */
    private SaPasswordEncoder.PasswordAlgorithm parsePasswordAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return SaPasswordEncoder.PasswordAlgorithm.BCRYPT;
        }
        try {
            return SaPasswordEncoder.PasswordAlgorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SaPasswordEncoder.PasswordAlgorithm.BCRYPT;
        }
    }

    /**
     * 密码编码器初始化器
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SaPasswordEncoderInitializer {
        private SaPasswordEncoder.PasswordAlgorithm algorithm;
    }
}
