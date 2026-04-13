package com.hc.framework.satoken.gateway.handler;

import com.hc.framework.common.model.DynamicAuthRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关动态路由权限提供者接口
 *
 * <p>实现此接口，从用户服务/权限服务动态获取 URL-权限映射规则。</p>
 * <p>优先级高于配置文件 auth-routes，配置文件作为兜底。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>从用户服务获取菜单-权限映射</li>
 *   <li>从权限服务获取 API-角色映射</li>
 *   <li>支持 RBAC 动态权限模型</li>
 * </ul>
 *
 * <ul>
 *   <li>精确路径 Map：对不含通配符的路径，O(1) 直接命中</li>
 *   <li>前缀分组索引：对通配符路径按一级前缀分组，减少匹配范围</li>
 *   <li>优先级排序缓存：避免每次排序</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 * @see DynamicAuthRoute
 */
public interface SaGatewayDynamicRouteProvider {

    /**
     * 加载所有动态路由权限规则
     *
     * <p>建议实现缓存机制，避免每次请求都调用远程服务。</p>
     *
     * @return 路由权限规则列表
     */
    List<DynamicAuthRoute> loadRoutes();

    /**
     * 匹配请求路径对应的权限规则
     *
     * <p>高性能实现：使用前缀索引 + 精确路径 Map 进行快速匹配。</p>
     *
     * <p>匹配策略：</p>
     * <ol>
     *   <li>优先精确匹配：对不含通配符的路径使用 HashMap，O(1) 复杂度</li>
     *   <li>前缀分组匹配：对通配符路径按一级前缀分组，减少匹配范围</li>
     *   <li>兜底遍历：前缀分组未命中时遍历剩余规则</li>
     * </ol>
     *
     * @param requestPath 请求路径，如 /api/user/info
     * @return 匹配到的路由规则，未匹配返回 null
     */
    default DynamicAuthRoute matchRoute(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            return null;
        }

        RouteMatchIndex index = getOrCreateMatchIndex();
        if (index == null) {
            return null;
        }

        // 1. 精确路径匹配 O(1)
        DynamicAuthRoute exactMatch = index.exactPathMap.get(requestPath);
        if (exactMatch != null) {
            return exactMatch;
        }

        // 2. 按一级前缀分组匹配
        String firstSegment = extractFirstSegment(requestPath);
        List<DynamicAuthRoute> prefixGroup = index.prefixGroupedRoutes.get(firstSegment);
        if (prefixGroup != null) {
            for (DynamicAuthRoute route : prefixGroup) {
                if (route.matches(requestPath)) {
                    return route;
                }
            }
        }

        // 3. 兜底：遍历通用通配符规则（如 /**）
        for (DynamicAuthRoute route : index.fallbackRoutes) {
            if (route.matches(requestPath)) {
                return route;
            }
        }

        return null;
    }

    /**
     * 获取或创建路由匹配索引
     *
     * <p>索引缓存于内存中，调用 clearCache() 后重建。</p>
     *
     * @return 路由匹配索引
     */
    default RouteMatchIndex getOrCreateMatchIndex() {
        List<DynamicAuthRoute> routes = loadRoutes();
        if (routes == null || routes.isEmpty()) {
            return null;
        }

        // 委托给实现类管理索引缓存
        return buildMatchIndex(routes);
    }

    /**
     * 构建路由匹配索引（供实现类调用）
     *
     * @param routes 路由规则列表
     * @return 匹配索引
     */
    default RouteMatchIndex buildMatchIndex(List<DynamicAuthRoute> routes) {
        // 按优先级排序（数值越小优先级越高）
        List<DynamicAuthRoute> sortedRoutes = routes.stream()
                .filter(r -> r.getEnabled() != null && r.getEnabled())
                .sorted((a, b) -> {
                    int pa = a.getPriority() != null ? a.getPriority() : 100;
                    int pb = b.getPriority() != null ? b.getPriority() : 100;
                    return Integer.compare(pa, pb);
                })
                .toList();

        Map<String, DynamicAuthRoute> exactPathMap = new HashMap<>();
        Map<String, List<DynamicAuthRoute>> prefixGroupedRoutes = new HashMap<>();
        List<DynamicAuthRoute> fallbackRoutes = new ArrayList<>();

        for (DynamicAuthRoute route : sortedRoutes) {
            String path = route.getPath();
            if (path == null || path.isEmpty()) {
                continue;
            }

            if (isExactPath(path)) {
                // 精确路径：直接放入 Map
                exactPathMap.put(path, route);
            } else if (isRootWildcard(path)) {
                // 根级别通配符（如 /**）：放入兜底列表
                fallbackRoutes.add(route);
            } else {
                // 按一级前缀分组
                String prefix = extractFirstSegment(path);
                prefixGroupedRoutes.computeIfAbsent(prefix, k -> new ArrayList<>()).add(route);
            }
        }

        return new RouteMatchIndex(exactPathMap, prefixGroupedRoutes, fallbackRoutes);
    }

    /**
     * 判断是否为精确路径（不含通配符）
     */
    private boolean isExactPath(String path) {
        return !path.contains("*") && !path.contains("?") && !path.contains("{");
    }

    /**
     * 判断是否为根级别通配符路径
     *
     * <p>如 /**、/* 等匹配所有路径的规则应放入兜底列表</p>
     */
    private boolean isRootWildcard(String path) {
        // 匹配 /** 或 /* 或类似的全局通配符
        if (path.equals("/**") || path.equals("/*")) {
            return true;
        }
        // 匹配 /**/xxx 这类以 /** 开头的路径
        return path.startsWith("/**/") || path.startsWith("/*/");
    }

    /**
     * 提取路径的一级前缀
     *
     * <p>例如：/api/user/info -> /api</p>
     */
    private String extractFirstSegment(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int secondSlash = path.indexOf('/', 1);
        if (secondSlash > 0) {
            return path.substring(0, secondSlash);
        }
        return path;
    }

    /**
     * 清除路由缓存（可选实现）
     *
     * <p>当权限数据变更时调用，用于主动刷新缓存。</p>
     */
    default void clearCache() {
        // 默认空实现
    }

    /**
     * 路由匹配索引
     *
     * <p>用于高性能路径匹配的数据结构。</p>
     */
    class RouteMatchIndex {
        /**
         * 精确路径 -> 路由规则（O(1) 查找）
         */
        final Map<String, DynamicAuthRoute> exactPathMap;

        /**
         * 一级前缀 -> 路由规则列表（减少匹配范围）
         */
        final Map<String, List<DynamicAuthRoute>> prefixGroupedRoutes;

        /**
         * 兜底路由规则（如 /** 等通用规则）
         */
        final List<DynamicAuthRoute> fallbackRoutes;

        RouteMatchIndex(
                Map<String, DynamicAuthRoute> exactPathMap,
                Map<String, List<DynamicAuthRoute>> prefixGroupedRoutes,
                List<DynamicAuthRoute> fallbackRoutes) {
            this.exactPathMap = Collections.unmodifiableMap(exactPathMap);
            this.prefixGroupedRoutes = Collections.unmodifiableMap(prefixGroupedRoutes);
            this.fallbackRoutes = Collections.unmodifiableList(fallbackRoutes);
        }
    }
}
