package com.hc.framework.satoken.handler;

import cn.dev33.satoken.annotation.SaCheckOr;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hc.framework.common.spi.PermissionCodeExtractor;

import java.lang.reflect.Method;

/**
 * Sa-Token 权限码提取器
 *
 * <p>从 Sa-Token 的权限注解中提取权限码，
 * 供 {@code DataPermissionAspect} 在未显式指定 {@code @DataPermission.value()} 时使用。</p>
 *
 * <p>提取顺序：</p>
 * <ol>
 *   <li>方法上的 {@code @SaCheckPermission} → 取第一个 value</li>
 *   <li>类上的 {@code @SaCheckPermission} → 取第一个 value</li>
 *   <li>方法上的 {@code @SaCheckOr} → 遍历内部注解取第一个有效 value</li>
 *   <li>均无 → 返回 null</li>
 * </ol>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class SaTokenPermissionCodeExtractor implements PermissionCodeExtractor {

    @Override
    public String extract(Method method, Class<?> clazz) {
        // 1. 方法上的 @SaCheckPermission
        SaCheckPermission methodP = method.getAnnotation(SaCheckPermission.class);
        if (methodP != null && methodP.value().length > 0) {
            return methodP.value()[0];
        }

        // 2. 类上的 @SaCheckPermission
        SaCheckPermission classP = clazz.getAnnotation(SaCheckPermission.class);
        if (classP != null && classP.value().length > 0) {
            return classP.value()[0];
        }

        // 3. @SaCheckOr 中取第一个
        SaCheckOr orP = method.getAnnotation(SaCheckOr.class);
        if (orP != null) {
            for (SaCheckPermission sp : orP.permission()) {
                if (sp.value().length > 0) {
                    return sp.value()[0];
                }
            }
        }

        return null;
    }
}
