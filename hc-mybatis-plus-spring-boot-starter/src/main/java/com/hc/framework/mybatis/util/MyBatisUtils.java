package com.hc.framework.mybatis.util;

import cn.hutool.core.util.StrUtil;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;

/**
 * MyBatis 工具类
 *
 * <p>提供 JSqlParser 表达式构建等工具方法。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class MyBatisUtils {

    /**
     * 构建 JSqlParser Column 表达式，自动处理表别名
     *
     * @param tableName  表名
     * @param tableAlias 表别名（可为 null）
     * @param columnName 列名
     * @return Column 表达式
     */
    public static Column buildColumn(String tableName, Alias tableAlias, String columnName) {
        String prefix;
        if (tableAlias != null && StrUtil.isNotBlank(tableAlias.getName())) {
            prefix = tableAlias.getName();
        } else if (StrUtil.isNotBlank(tableName)) {
            prefix = tableName;
        } else {
            prefix = null;
        }
        if (prefix != null) {
            return new Column(prefix + "." + columnName);
        }
        return new Column(columnName);
    }
}
