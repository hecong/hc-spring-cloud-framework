package com.hc.framework.satoken.config;

import com.hc.framework.satoken.util.SaPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 配置刷新支持
 *
 * <p>支持 Nacos/Apollo 配置中心的动态刷新。</p>
 *
 * <p>可动态刷新的配置项：</p>
 * <ul>
 *   <li>Token 过期时间</li>
 *   <li>密码加密算法</li>
 *   <li>权限规则</li>
 *   <li>登录日志开关</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 在 Nacos/Apollo 中配置：
 * hc:
 *   satoken:
 *     token:
 *       timeout: 86400
 *     password:
 *       algorithm: BCRYPT
 *     auth-log:
 *       login-log-enabled: true
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>需要引入 spring-cloud-starter-alibaba-nacos-config 或 spring-cloud-starter-bus</li>
 *   <li>配置修改后会自动刷新，无需重启服务</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SaTokenProperties.class)
@ConditionalOnClass(name = "org.springframework.cloud.context.config.annotation.RefreshScope")
public class SaTokenRefreshConfiguration {

    private final SaTokenProperties saTokenProperties;

    /**
     * 密码编码器（支持动态刷新）
     *
     * <p>当配置中心的密码算法变更时，自动切换编码算法。</p>
     */
    @Bean
    @RefreshScope
    @Primary
    public SaPasswordEncoderHolder saPasswordEncoderHolder() {
        String algorithm = saTokenProperties.getPassword().getAlgorithm();
        SaPasswordEncoder.PasswordAlgorithm passwordAlgorithm = parseAlgorithm(algorithm);
        SaPasswordEncoder.setDefaultAlgorithm(passwordAlgorithm);
        log.info("Sa-Token 密码编码器已初始化（动态刷新支持），算法: {}", passwordAlgorithm);
        return new SaPasswordEncoderHolder(passwordAlgorithm);
    }

    /**
     * 解析密码算法
     */
    private SaPasswordEncoder.PasswordAlgorithm parseAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return SaPasswordEncoder.PasswordAlgorithm.BCRYPT;
        }
        try {
            return SaPasswordEncoder.PasswordAlgorithm.valueOf(algorithm.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的密码算法: {}，使用默认算法 BCRYPT", algorithm);
            return SaPasswordEncoder.PasswordAlgorithm.BCRYPT;
        }
    }

    /**
     * 密码编码器持有者
     *
     * <p>用于支持 @RefreshScope 动态刷新。</p>
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SaPasswordEncoderHolder {
        private SaPasswordEncoder.PasswordAlgorithm algorithm;
    }
}
