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
     * 优先级数值，越大越宽
     */
    public int priority() {
        return this.ordinal();
    }
}
