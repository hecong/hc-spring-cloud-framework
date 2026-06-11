package com.hc.framework.mybatis.constant;

/**
 * 数据权限常量
 *
 * @author hc-framework
 * @since 1.0.0
 */
public final class DataPermConstants {

    // ==================== 表名 ====================

    /** 系统部门表 */
    public static final String TABLE_SYS_DEPT = "sys_dept";
    /** 系统用户表 */
    public static final String TABLE_SYS_USER = "sys_users";

    // ==================== 列名 ====================

    /** 部门ID列 */
    public static final String COLUMN_DEPT_ID = "dept_id";
    /** 用户ID列 */
    public static final String COLUMN_USER_ID = "user_id";
    /** 租户ID列 */
    public static final String COLUMN_TENANT_ID = "tenant_id";

    // ==================== 限制 ====================

    /** 部门ID展开上限（超出告警并截断） */
    public static final int MAX_DEPT_EXPAND_SIZE = 500;

    private DataPermConstants() {
    }
}
