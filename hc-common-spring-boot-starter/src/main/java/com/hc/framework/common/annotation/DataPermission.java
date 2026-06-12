package com.hc.framework.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限标记注解
 *
 * <p>标注在 Controller 方法或类上，启用数据权限过滤。
 * 权限码可通过 {@link #value()} 显式指定，或通过
 * {@link com.hc.framework.common.spi.PermissionCodeExtractor} 从认证框架注解自动提取。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 方式 1：显式指定权限码（不依赖任何认证框架注解）
 * @DataPermission("user:list")
 * @GetMapping("/list")
 * public Result<List<UserResponse>> list() { ... }
 *
 * // 方式 2：自动提取（和 @SaCheckPermission 配合）
 * @SaCheckPermission("user:list")
 * @DataPermission
 * @GetMapping("/list")
 * public Result<List<UserResponse>> list() { ... }
 *
 * // 方式 3：跳过数据权限
 * @DataPermission(enable = false)
 * @GetMapping("/export")
 * public void export() { ... }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 权限码
     *
     * <p>显式指定时优先使用；为空字符串时（默认），
     * 通过 {@code PermissionCodeExtractor} SPI 自动从认证框架注解提取。</p>
     *
     * @return 权限码字符串
     */
    String value() default "";

    /**
     * 是否启用数据权限过滤
     *
     * @return true 启用，false 跳过
     */
    boolean enable() default true;
}
