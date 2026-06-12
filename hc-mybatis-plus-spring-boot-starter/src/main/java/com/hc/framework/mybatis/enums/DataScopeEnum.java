package com.hc.framework.mybatis.enums;

/**
 * 数据权限范围枚举
 *
 * <p>优先级从高到低：ALL > DEPT_AND_CHILDREN > CUSTOM_DEPT > CURRENT_DEPT > SELF</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public enum DataScopeEnum {

    /** 全部数据权限 — 最高优先级 */
    ALL("全部数据权限"),

    /** 当前部门及以下数据权限 */
    DEPT_AND_CHILDREN("当前部门及以下数据权限"),

    /** 指定部门数据权限 */
    CUSTOM_DEPT("指定部门数据权限"),

    /** 当前部门数据权限 */
    CURRENT_DEPT("当前部门数据权限"),

    /** 仅自己数据权限 — 最低优先级 */
    SELF("仅自己数据权限");

    private final String description;

    DataScopeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 优先级数值，值越小权限范围越宽
     *
     * <p>优先级从高到低：ALL(0) &gt; DEPT_AND_CHILDREN(1) &gt; CUSTOM_DEPT(2)
     * &gt; CURRENT_DEPT(3) &gt; SELF(4)</p>
     *
     * <p>多角色合并规则：取最宽数据范围（priority 最小的），
     * 确保用户能看到所有有权访问的数据。</p>
     */
    public int priority() {
        return this.ordinal();
    }
}
