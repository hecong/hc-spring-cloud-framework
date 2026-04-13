package com.hc.framework.common.constant;

/**
 * 全局缓存KEY
 * @author hecong
 * @since 2026/4/13 10:32
 */
public interface CacheConstants {

    /**
     * 缓存 Key 前缀
     */
    String CACHE_PREFIX = "satoken:cache:";

    /**
     * 角色缓存后缀
     */
    String ROLES_SUFFIX = CACHE_PREFIX + "roles:";

    /**
     * 权限缓存后缀
     */
    String PERMISSIONS_SUFFIX = CACHE_PREFIX + "permissions:";


}
