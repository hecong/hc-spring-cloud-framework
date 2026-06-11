package com.hc.framework.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限标记注解
 *
 * <p>标注在 Controller 方法或类上，启用数据权限过滤。
 * 权限码从同方法/类的 {@code @SaCheckPermission} 注解自动提取，
 * 无需在此重复声明。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @SaCheckPermission("user:list")   // 权限码从这里提取
 * @DataPermission                   // 启用数据权限
 * @GetMapping("/list")
 * public Result<List<UserResponse>> list() { ... }
 *
 * @DataPermission(enable = false)   // 跳过数据权限
 * @SaCheckPermission("user:export")
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
     * 是否启用数据权限过滤
     *
     * @return true 启用，false 跳过
     */
    boolean enable() default true;
}
