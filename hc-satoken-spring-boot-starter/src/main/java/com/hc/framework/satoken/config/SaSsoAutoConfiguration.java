package com.hc.framework.satoken.config;

import com.hc.framework.satoken.config.SaTokenProperties.SsoConfig;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token SSO 单点登录自动配置类
 *
 * <p>封装 SSO 核心配置，支持单点登录、注销、校验。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>自动配置 SSO 认证中心地址</li>
 *   <li>支持 Ticket 校验</li>
 *   <li>支持单点登录/注销</li>
 *   <li>支持 REST API 模式 SSO</li>
 * </ul>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     sso:
 *       enabled: true
 *       server-url: http://sso.example.com
 *       client-url: http://app.example.com
 *       secret: your-sso-secret
 * }</pre>
 *
 * <p>注意：需要引入 sa-token-sso 依赖才能生效：</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>cn.dev33</groupId>
 *     <artifactId>sa-token-sso</artifactId>
 *     <version>${satoken.version}</version>
 * </dependency>
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SaTokenProperties.class)
@ConditionalOnClass(name = "cn.dev33.satoken.sso.SaSsoManager")
@ConditionalOnProperty(prefix = "hc.satoken.sso", name = "enabled", havingValue = "true")
public class SaSsoAutoConfiguration {

    private final SaTokenProperties properties;

    /**
     * 构造器注入
     */
    public SaSsoAutoConfiguration(SaTokenProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化 SSO 配置
     */
    @Bean
    public SsoConfigHolder ssoConfigHolder() {
        SsoConfig sso = properties.getSso();

        log.info("=== Sa-Token SSO 配置 ===");
        log.info("SSO Server URL: {}", sso.getServerUrl());
        log.info("SSO Client URL: {}", sso.getClientUrl());
        log.info("SSO Login Path: {}", sso.getLoginPath());
        log.info("SSO Logout Path: {}", sso.getLogoutPath());
        log.info("Ticket Timeout: {}s", sso.getTicketTimeout());

        return new SsoConfigHolder(sso);
    }

    /**
     * SSO 配置持有者
     *
     * <p>提供 SSO 配置的便捷访问方法。</p>
     */
    public static class SsoConfigHolder {

        private final SsoConfig config;

        public SsoConfigHolder(SsoConfig config) {
            this.config = config;
        }

        /**
         * 获取 SSO Server 地址
         */
        public String getServerUrl() {
            return config.getServerUrl();
        }

        /**
         * 获取 SSO Client 地址
         */
        public String getClientUrl() {
            return config.getClientUrl();
        }

        /**
         * 获取 SSO 登录路径
         */
        public String getLoginPath() {
            return config.getLoginPath();
        }

        /**
         * 获取 SSO 注销路径
         */
        public String getLogoutPath() {
            return config.getLogoutPath();
        }

        /**
         * 获取 SSO 密钥
         */
        public String getSecret() {
            return config.getSecret();
        }

        /**
         * 获取 Ticket 有效期
         */
        public Long getTicketTimeout() {
            return config.getTicketTimeout();
        }

        /**
         * 获取完整登录 URL
         *
         * @param redirectUrl 登录成功后跳转地址
         * @return 完整登录 URL
         */
        public String getFullLoginUrl(String redirectUrl) {
            String serverUrl = config.getServerUrl();
            String loginPath = config.getLoginPath();
            return buildUrl(redirectUrl, serverUrl, loginPath);
        }

        /**
         * 获取完整注销 URL
         *
         * @param redirectUrl 注销后跳转地址
         * @return 完整注销 URL
         */
        public String getFullLogoutUrl(String redirectUrl) {
            String serverUrl = config.getServerUrl();
            String logoutPath = config.getLogoutPath();
            return buildUrl(redirectUrl, serverUrl, logoutPath);
        }

        @NotNull
        private String buildUrl(String redirectUrl, String serverUrl, String path) {
            StringBuilder url = new StringBuilder(serverUrl);
            if (!serverUrl.endsWith("/") && !path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                url.append("?redirect=").append(redirectUrl);
            }
            return url.toString();
        }
    }
}
