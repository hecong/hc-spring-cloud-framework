package com.hc.framework.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据权限上下文持有者
 *
 * <p>在 Controller AOP 切面中设置权限码，MyBatis 拦截器中读取。
 * 使用 {@link TransmittableThreadLocal} 配合 {@link Deque} 栈结构，
 * 支持嵌套调用和异步场景（线程池、@Async）下的上下文传递。</p>
 *
 * <p>生命周期：请求进入 Controller 前设置 → MyBatis 查询时读取 → 请求结束后清理</p>
 *
 * <p>嵌套调用支持：</p>
 * <pre>{@code
 * // 外层请求 user:list
 * DataPermissionContextHolder.set("user:list");
 * try {
 *     // 内层 service 调用 user:detail，设置自己的权限码
 *     DataPermissionContextHolder.set("user:detail");
 *     try {
 *         // DataPermissionContextHolder.get() → "user:detail"
 *     } finally {
 *         DataPermissionContextHolder.clear(); // 弹出 "user:detail"，栈顶回到 "user:list"
 *     }
 *     // DataPermissionContextHolder.get() → "user:list"
 * } finally {
 *     DataPermissionContextHolder.clear(); // 弹出 "user:list"，栈为空
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<Deque<String>> STACK =
            TransmittableThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 设置当前请求的权限码（压入栈顶）
     *
     * @param permissionCode 菜单权限编码
     */
    public static void set(String permissionCode) {
        STACK.get().push(permissionCode);
    }

    /**
     * 获取当前请求的权限码（查看栈顶）
     *
     * @return 权限码，未设置时返回 null
     */
    public static String get() {
        Deque<String> deque = STACK.get();
        return deque.isEmpty() ? null : deque.peek();
    }

    /**
     * 清理当前层的权限码（弹出栈顶）
     *
     * <p>仅弹出当前层的权限码，不影响外层嵌套调用的权限码。
     * 应在 finally 块中调用，与 set() 一一对应。</p>
     */
    public static void clear() {
        Deque<String> deque = STACK.get();
        if (!deque.isEmpty()) {
            deque.poll();
        }
        // 栈为空时移除整个 ThreadLocal，防止内存泄漏
        if (deque.isEmpty()) {
            STACK.remove();
        }
    }
}
