package com.hc.framework.common.spi;

import java.lang.reflect.Method;

/**
 * 权限码提取器 SPI
 *
 * <p>从目标方法/类上提取权限码。各认证框架模块（如 sa-token）提供对应实现，
 * 从框架自身的权限注解（如 {@code @SaCheckPermission}）中提取权限码。</p>
 *
 * <p>当 {@code @DataPermission} 注解未显式指定 {@code value()} 时，
 * 框架会通过此 SPI 自动提取权限码。</p>
 *
 * <p>实现示例（Sa-Token）：</p>
 * <pre>{@code
 * public class SaTokenPermissionCodeExtractor implements PermissionCodeExtractor {
 *     @Override
 *     public String extract(Method method, Class<?> targetClass) {
 *         SaCheckPermission p = method.getAnnotation(SaCheckPermission.class);
 *         if (p != null && p.value().length > 0) {
 *             return p.value()[0];
 *         }
 *         p = targetClass.getAnnotation(SaCheckPermission.class);
 *         if (p != null && p.value().length > 0) {
 *             return p.value()[0];
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface PermissionCodeExtractor {

    /**
     * 从目标方法提取权限码
     *
     * @param method      目标方法
     * @param targetClass 目标类
     * @return 权限码，无法提取时返回 null
     */
    String extract(Method method, Class<?> targetClass);
}
