package com.hc.framework.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token Web MVC 配置类
 *
 * <p>使用 Sa-Token 内置的 SaInterceptor，同时支持：</p>
 * <ul>
 *   <li>注解鉴权：@SaCheckLogin / @SaCheckRole / @SaCheckPermission</li>
 *   <li>路由鉴权：排除路径以外的请求默认需要登录</li>
 * </ul>
 *
 * <p>排除路径由 SaTokenProperties.getAllExcludePaths() 提供，
 * 包含 Swagger、登录接口、用户自定义排除路径等。</p>
 *
 * <p>启用条件：hc.satoken.enabled = true（默认）</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "hc.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenWebMvcConfiguration implements WebMvcConfigurer {

    private final SaTokenProperties saTokenProperties;

    public SaTokenWebMvcConfiguration(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 排除路径不需要任何校验，其余路径默认需要登录
            SaRouter.notMatch(saTokenProperties.getAllExcludePaths())
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**")
          .excludePathPatterns(saTokenProperties.getAllExcludePaths())
          .order(0);
    }
}
