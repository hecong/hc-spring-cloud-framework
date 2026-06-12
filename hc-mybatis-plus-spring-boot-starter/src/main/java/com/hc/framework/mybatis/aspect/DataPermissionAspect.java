package com.hc.framework.mybatis.aspect;

import com.hc.framework.common.annotation.DataPermission;
import com.hc.framework.common.context.DataPermissionContextHolder;
import com.hc.framework.common.spi.PermissionCodeExtractor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 数据权限切面
 *
 * <p>拦截标注了 {@link DataPermission} 注解的 Controller 方法，
 * 提取权限码并设置到 {@link DataPermissionContextHolder} 中，
 * 供 MyBatis 数据权限拦截器读取。</p>
 *
 * <p>权限码提取优先级：</p>
 * <ol>
 *   <li>{@code @DataPermission.value()} 显式指定 → 直接使用</li>
 *   <li>{@code value()} 为空 → 通过 {@link PermissionCodeExtractor} SPI 自动提取</li>
 *   <li>无法提取 → 跳过数据权限过滤，记录警告日志</li>
 * </ol>
 *
 * <p>注解查找优先级：方法级 > 类级</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Aspect
public class DataPermissionAspect {

    private final PermissionCodeExtractor permissionCodeExtractor;

    public DataPermissionAspect(PermissionCodeExtractor permissionCodeExtractor) {
        this.permissionCodeExtractor = permissionCodeExtractor;
    }

    @Around("@within(com.hc.framework.common.annotation.DataPermission) || " +
            "@annotation(com.hc.framework.common.annotation.DataPermission)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        DataPermission methodDp = method.getAnnotation(DataPermission.class);
        DataPermission classDp = pjp.getTarget().getClass().getAnnotation(DataPermission.class);
        DataPermission effective = (methodDp != null) ? methodDp : classDp;

        if (effective == null || !effective.enable()) {
            return pjp.proceed();
        }

        // 优先取 @DataPermission.value()，为空则走 SPI
        String permissionCode = effective.value();
        if (permissionCode.isEmpty()) {
            permissionCode = permissionCodeExtractor.extract(method, pjp.getTarget().getClass());
        }

        if (permissionCode == null) {
            log.warn("[数据权限] 未提取到权限码，跳过数据权限过滤 on {}.{}",
                    pjp.getTarget().getClass().getSimpleName(), method.getName());
            return pjp.proceed();
        }

        DataPermissionContextHolder.set(permissionCode);
        try {
            return pjp.proceed();
        } finally {
            DataPermissionContextHolder.clear();
        }
    }
}
