package com.hc.framework.common.constant;

/**
 * 系统通用常量
 *
 * <p>统一定义系统级别的通用常量，包括通用状态、布尔值、符号等，
 * 避免魔法值散落在业务代码中。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * if (SystemConstants.STATUS_ENABLE.equals(user.getStatus())) { ... }
 * String result = flag ? SystemConstants.YES : SystemConstants.NO;
 * }</pre>
 *
 * @author hc-framework
 */
public interface SystemConstants {

    // ==================== 通用状态 ====================

    /**
     * 启用状态
     */
    Integer STATUS_ENABLE = 1;

    /**
     * 禁用状态
     */
    Integer STATUS_DISABLE = 0;

    /**
     * 删除标志：已删除
     */
    Integer DELETED = 1;

    /**
     * 删除标志：未删除
     */
    Integer NOT_DELETED = 0;

    // ==================== 是/否 ====================

    /**
     * 是（整数）
     */
    Integer YES_INT = 1;

    /**
     * 否（整数）
     */
    Integer NO_INT = 0;

    /**
     * 是（字符串）
     */
    String YES = "Y";

    /**
     * 否（字符串）
     */
    String NO = "N";

    // ==================== 通用符号 ====================

    /**
     * 空字符串
     */
    String EMPTY = "";

    /**
     * 英文逗号
     */
    String COMMA = ",";

    /**
     * 英文分号
     */
    String SEMICOLON = ";";

    /**
     * 英文冒号
     */
    String COLON = ":";

    /**
     * 英文点号
     */
    String DOT = ".";

    /**
     * 下划线
     */
    String UNDERSCORE = "_";

    /**
     * 短横线
     */
    String HYPHEN = "-";

    /**
     * 斜杠
     */
    String SLASH = "/";

    /**
     * 换行符
     */
    String LINE_BREAK = "\n";

    // ==================== 数字常量 ====================

    /**
     * 数字零
     */
    Integer ZERO = 0;

    /**
     * 数字一
     */
    Integer ONE = 1;

    // ==================== 默认分页 ====================

    /**
     * 默认页码
     */
    int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页条数
     */
    int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页条数（防止全量查询）
     */
    int MAX_PAGE_SIZE = 1000;

    // ==================== 编码 ====================

    /**
     * UTF-8 编码
     */
    String CHARSET_UTF8 = "UTF-8";
}
