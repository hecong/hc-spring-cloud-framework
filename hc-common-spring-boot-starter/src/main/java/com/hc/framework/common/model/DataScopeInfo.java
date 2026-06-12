package com.hc.framework.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 有效数据范围信息
 *
 * <p>由 {@link com.hc.framework.common.spi.DataScopeProvider} 计算后返回，
 * 包含当前用户对指定菜单的最终数据过滤条件。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否全部数据权限 */
    @Builder.Default
    private boolean all = false;

    /** 是否包含仅自己过滤 */
    @Builder.Default
    private boolean self = false;

    /** 可见的部门ID集合（已展开子部门、已去重） */
    @Builder.Default
    private Set<Long> deptIds = new LinkedHashSet<>();

    /** 租户ID集合（预留多租户） */
    @Builder.Default
    private Set<Long> tenantIds = new LinkedHashSet<>();

    /** dept 条件和 user 条件之间的连接符 */
    @Builder.Default
    private Connector connector = Connector.OR;

    /**
     * 条件连接符
     */
    public enum Connector {
        /** 部门条件和自条件取并集 */
        OR,
        /** 部门条件和自条件取交集 */
        AND
    }
}
