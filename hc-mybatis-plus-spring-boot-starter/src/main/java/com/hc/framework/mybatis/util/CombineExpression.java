package com.hc.framework.mybatis.util;

import com.hc.framework.mybatis.model.DataScopeInfo;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式组合工具类
 *
 * <p>用于将多个 JSqlParser Expression 按指定连接符（AND/OR）组合，
 * 外层自动包裹 Parenthesis。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class CombineExpression {

    private final List<Expression> expressions = new ArrayList<>();
    private final DataScopeInfo.Connector connector;

    public CombineExpression(DataScopeInfo.Connector connector) {
        this.connector = connector;
    }

    /**
     * 添加一个表达式（为 null 则跳过）
     */
    public CombineExpression combine(Expression expr) {
        if (expr != null) {
            expressions.add(expr);
        }
        return this;
    }

    /**
     * 获取组合后的表达式，外层自动加括号
     *
     * @return 组合后的 Expression，无任何条件时返回 null
     */
    public Expression getParenthesis() {
        if (expressions.isEmpty()) {
            return null;
        }
        if (expressions.size() == 1) {
            return new Parenthesis(expressions.get(0));
        }

        Expression result = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            if (connector == DataScopeInfo.Connector.AND) {
                result = new AndExpression(result, expressions.get(i));
            } else {
                result = new OrExpression(result, expressions.get(i));
            }
        }
        return new Parenthesis(result);
    }
}
