package com.hc.framework.mybatis.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.hc.framework.common.spi.UserIdProvider;
import com.hc.framework.mybatis.constant.DataPermConstants;
import com.hc.framework.mybatis.handler.DataPermissionContextHolder;
import com.hc.framework.mybatis.model.DataScopeInfo;
import com.hc.framework.mybatis.spi.DataScopeProvider;
import com.hc.framework.mybatis.util.CombineExpression;
import com.hc.framework.mybatis.util.MyBatisUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 部门数据权限处理器
 *
 * <p>实现 MyBatis-Plus 的 {@link MultiDataPermissionHandler}，基于 JSqlParser
 * 安全构建 SQL 表达式，杜绝字符串拼接注入。</p>
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>自动发现包含 dept_id / user_id / tenant_id 列的表</li>
 *   <li>根据用户数据范围（ALL / DEPT_AND_CHILDREN / CUSTOM_DEPT / CURRENT_DEPT / SELF）
 *       自动追加 WHERE 条件</li>
 *   <li>超级管理员跳过过滤</li>
 *   <li>表白名单跳过过滤</li>
 *   <li>JSqlParser 解析异常降级</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
public class DeptDataPermissionHandler implements MultiDataPermissionHandler, SmartInitializingSingleton {

    private final DataScopeProvider dataScopeProvider;
    private final UserIdProvider userIdProvider;

    /** 含 dept_id 列的表名集合 */
    private final Set<String> deptTables = ConcurrentHashMap.newKeySet();
    /** 含 user_id 列的表名集合 */
    private final Set<String> userTables = ConcurrentHashMap.newKeySet();
    /** 含 tenant_id 列的表名集合 */
    private final Set<String> tenantTables = ConcurrentHashMap.newKeySet();

    /** 表白名单：这些表不进行数据权限过滤 */
    private static final Set<String> SKIP_TABLES = Set.of(
        "sys_permission", "sys_role", "sys_dept", "sys_config",
        "sys_role_permission", "sys_user_role", "sys_role_data_scope",
        "sys_role_permission_data_scope", "sys_role_perm_dept_scope"
    );

    public DeptDataPermissionHandler(DataScopeProvider dataScopeProvider,
                                       UserIdProvider userIdProvider) {
        this.dataScopeProvider = dataScopeProvider;
        this.userIdProvider = userIdProvider;
    }

    // ==================== MultiDataPermissionHandler ====================

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        // 1. 从 ThreadLocal 获取 Controller @DataPermission 注解透传的权限码
        String permissionCode = DataPermissionContextHolder.get();
        if (permissionCode == null) {
            return null;
        }

        // 2. 获取当前用户ID（空指针保护）
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        // 3. 表名白名单跳过
        String tableName = table.getName();
        if (SKIP_TABLES.contains(tableName)) {
            return null;
        }

        // 4. 获取数据范围（try-catch 降级；超级管理员逻辑由 DataScopeProvider 内部处理）
        DataScopeInfo scopeInfo;
        try {
            scopeInfo = dataScopeProvider.getDataScope(userId, permissionCode);
        } catch (Exception e) {
            log.error("[数据权限] 获取数据范围失败 userId={} permissionCode={}",
                userId, permissionCode, e);
            return null; // 降级：不追加条件，保证业务可用
        }

        // 5. ALL 范围 → 无需追加条件（租户隔离也由 provider 在 DataScopeInfo 中处理）
        if (scopeInfo.isAll() && CollUtil.isEmpty(scopeInfo.getTenantIds())) {
            return null;
        }

        // 6. JSqlParser 构建表达式（try-catch 降级）
        try {
            return buildExpression(userId, table, scopeInfo);
        } catch (Exception e) {
            log.error("[数据权限] SQL表达式构建失败 userId={} table={} scopeInfo={}",
                userId, tableName, scopeInfo, e);
            return null; // 降级：不追加条件
        }
    }

    // ==================== 表达式构建 ====================

    private Expression buildExpression(Long userId, Table table, DataScopeInfo scopeInfo) {
        String tableName = table.getName();
        Alias alias = table.getAlias();

        // ALL → 只追加租户条件（如有多租户）
        if (scopeInfo.isAll()) {
            return buildTenantExpression(tableName, alias, scopeInfo.getTenantIds());
        }

        // 无部门ID且不需要自过滤 → 返回 1=0（查不到数据）
        if (CollUtil.isEmpty(scopeInfo.getDeptIds()) && !scopeInfo.isSelf()) {
            return new EqualsTo(new LongValue(1), new LongValue(0));
        }

        Expression deptExpr = buildDeptExpression(tableName, alias, scopeInfo.getDeptIds());
        Expression userExpr = buildUserExpression(tableName, alias, scopeInfo.isSelf(), userId);
        Expression tenantExpr = buildTenantExpression(tableName, alias, scopeInfo.getTenantIds());

        return new CombineExpression(scopeInfo.getConnector())
            .combine(deptExpr)
            .combine(userExpr)
            .combine(tenantExpr)
            .getParenthesis();
    }

    /**
     * 构建部门过滤表达式：dept_id IN (?, ?, ...)
     */
    private Expression buildDeptExpression(String tableName, Alias alias, Set<Long> deptIds) {
        if (!deptTables.contains(tableName) || CollUtil.isEmpty(deptIds)) {
            return null;
        }
        String column = DataPermConstants.TABLE_SYS_DEPT.equals(tableName)
            ? "id" : DataPermConstants.COLUMN_DEPT_ID;
        Column col = MyBatisUtils.buildColumn(tableName, alias, column);
        List<LongValue> values = deptIds.stream().map(LongValue::new).collect(Collectors.toList());
        return new InExpression(col, new ExpressionList<>(values));
    }

    /**
     * 构建自过滤表达式：user_id = ?
     */
    private Expression buildUserExpression(String tableName, Alias alias, boolean self, Long userId) {
        if (!userTables.contains(tableName) || !self) {
            return null;
        }
        String column = DataPermConstants.TABLE_SYS_USER.equals(tableName)
            ? "id" : DataPermConstants.COLUMN_USER_ID;
        return new EqualsTo(
            MyBatisUtils.buildColumn(tableName, alias, column),
            new LongValue(userId)
        );
    }

    /**
     * 构建租户过滤表达式：tenant_id IN (?, ?, ...)
     */
    private Expression buildTenantExpression(String tableName, Alias alias, Set<Long> tenantIds) {
        if (!tenantTables.contains(tableName) || CollUtil.isEmpty(tenantIds)) {
            return null;
        }
        Column col = MyBatisUtils.buildColumn(tableName, alias, DataPermConstants.COLUMN_TENANT_ID);
        List<LongValue> values = tenantIds.stream().map(LongValue::new).collect(Collectors.toList());
        return new InExpression(col, new ExpressionList<>(values));
    }

    // ==================== 表自动发现 ====================

    @Override
    public void afterSingletonsInstantiated() {
        refreshTableCache();
        log.info("[数据权限] 表扫描完成 deptTables={} userTables={} tenantTables={}",
            deptTables.size(), userTables.size(), tenantTables.size());
    }

    /**
     * 动态刷新表缓存（支持运行时新增表/字段，运维接口可调用）
     */
    public void refreshTableCache() {
        deptTables.clear();
        userTables.clear();
        tenantTables.clear();
        for (TableInfo ti : TableInfoHelper.getTableInfos()) {
            for (TableFieldInfo field : ti.getFieldList()) {
                String col = field.getColumn();
                if (DataPermConstants.COLUMN_DEPT_ID.equals(col)) {
                    deptTables.add(ti.getTableName());
                } else if (DataPermConstants.COLUMN_USER_ID.equals(col)) {
                    userTables.add(ti.getTableName());
                } else if (DataPermConstants.COLUMN_TENANT_ID.equals(col)) {
                    tenantTables.add(ti.getTableName());
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前登录用户ID（通过 UserIdProvider SPI，由 sa-token 模块提供实现）
     */
    private Long getCurrentUserId() {
        try {
            String userIdStr = userIdProvider.get();
            if (StrUtil.isBlank(userIdStr)) {
                return null;
            }
            return Long.parseLong(userIdStr);
        } catch (Exception e) {
            return null;
        }
    }
}
