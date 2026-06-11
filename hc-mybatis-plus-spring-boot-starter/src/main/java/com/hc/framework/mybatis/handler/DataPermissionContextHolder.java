package com.hc.framework.mybatis.handler;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 数据权限上下文持有者
 *
 * <p>在 Controller AOP 切面中设置权限码，MyBatis 拦截器中读取。
 * 使用 TransmittableThreadLocal 支持异步场景（线程池、@Async）下的上下文传递。</p>
 *
 * <p>生命周期：请求进入 Controller 前设置 → MyBatis 查询时读取 → 请求结束后清理</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<String> PERMISSION_CODE = new TransmittableThreadLocal<>();

    /**
     * 设置当前请求的权限码
     *
     * @param permissionCode 菜单权限编码（来自 @SaCheckPermission）
     */
    public static void set(String permissionCode) {
        PERMISSION_CODE.set(permissionCode);
    }

    /**
     * 获取当前请求的权限码
     *
     * @return 权限码，未设置时返回 null
     */
    public static String get() {
        return PERMISSION_CODE.get();
    }

    /**
     * 清理当前线程的权限码（防止内存泄漏）
     */
    public static void clear() {
        PERMISSION_CODE.remove();
    }
}
