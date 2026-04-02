package com.hc.framework.satoken.config;

import com.hc.framework.satoken.interceptor.SaTokenUrlInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token Web MVC 配置类
 *
 * <p>注册 URL 权限拦截器，实现基于配置文件的路径权限控制。</p>
 *
 * <p>拦截器配置：</p>
 * <ul>
 *   <li>拦截路径：/**（所有路径）</li>
 *   <li>排除路径：由 SaTokenProperties.getAllExcludePaths() 提供</li>
 *   <li>优先级：较高（在业务拦截器之前执行）</li>
 * </ul>
 *
 * <p>启用条件：</p>
 * <ul>
 *   <li>hc.satoken.enabled = true（默认）</li>
 *   <li>hc.satoken.permission.enabled = true（默认）</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "hc.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenWebMvcConfiguration implements WebMvcConfigurer {

    private final SaTokenProperties saTokenProperties;

    /**
     * 构造器注入
     */
    public SaTokenWebMvcConfiguration(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    /**
     * 注册 Sa-Token URL 权限拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 权限校验未启用，不注册拦截器
        if (!Boolean.TRUE.equals(saTokenProperties.getPermission().getEnabled())) {
            return;
        }

        SaTokenUrlInterceptor interceptor = new SaTokenUrlInterceptor(saTokenProperties);

        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(saTokenProperties.getAllExcludePaths())
                .order(0); // 最高优先级，确保在业务拦截器之前执行
    }
}
