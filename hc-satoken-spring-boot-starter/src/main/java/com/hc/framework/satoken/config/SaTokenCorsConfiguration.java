package com.hc.framework.satoken.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Sa-Token 跨域自动配置
 *
 * <p>为前后端分离场景提供跨域支持，自动将 Sa-Token 的 Token 加入跨域允许头。</p>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * hc:
 *   satoken:
 *     frontend:
 *       cors-enabled: true
 *       cors-token-header: true
 *       cors-allowed-origins: "*"
 *       cors-allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
 *       cors-allowed-headers: "*"
 *       cors-max-age: 3600
 *       cors-allow-credentials: true
 * }</pre>
 *
 * <p>功能说明：</p>
 * <ul>
 *   <li>自动添加 Token 名称到 Access-Control-Expose-Headers</li>
 *   <li>支持配置允许的来源、方法、头</li>
 *   <li>支持预检请求缓存</li>
 *   <li>支持携带凭证</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hc.satoken.frontend", name = "cors-enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenCorsConfiguration {

    private final SaTokenProperties saTokenProperties;

    /**
     * 跨域过滤器
     *
     * <p>优先级设置为最高，确保在其他过滤器之前执行。</p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(CorsFilter.class)
    public CorsFilter saTokenCorsFilter() {
        log.info("Sa-Token 跨域配置已启用");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCorsConfiguration());

        return new CorsFilter(source);
    }

    /**
     * 构建跨域配置
     */
    private CorsConfiguration buildCorsConfiguration() {
        SaTokenProperties.FrontendConfig frontendConfig = saTokenProperties.getFrontend();

        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源
        String allowedOrigins = frontendConfig.getCorsAllowedOrigins();
        if ("*".equals(allowedOrigins)) {
            config.addAllowedOriginPattern("*");
        } else {
            for (String origin : allowedOrigins.split(",")) {
                config.addAllowedOrigin(origin.trim());
            }
        }

        // 允许的方法
        String allowedMethods = frontendConfig.getCorsAllowedMethods();
        for (String method : allowedMethods.split(",")) {
            config.addAllowedMethod(method.trim());
        }

        // 允许的头
        String allowedHeaders = frontendConfig.getCorsAllowedHeaders();
        if ("*".equals(allowedHeaders)) {
            config.addAllowedHeader("*");
        } else {
            for (String header : allowedHeaders.split(",")) {
                config.addAllowedHeader(header.trim());
            }
        }

        // 自动将 Token 加入允许头
        if (Boolean.TRUE.equals(frontendConfig.getCorsTokenHeader())) {
            String tokenName = saTokenProperties.getToken().getName();
            config.addExposedHeader(tokenName);
            config.addExposedHeader("Authorization");
            config.addExposedHeader("X-Token");
            log.info("Sa-Token 已将 Token 头 [{}] 加入跨域暴露头", tokenName);
        }

        // 预检请求缓存时间
        config.setMaxAge(frontendConfig.getCorsMaxAge());

        // 是否允许携带凭证
        config.setAllowCredentials(frontendConfig.getCorsAllowCredentials());

        return config;
    }
}
