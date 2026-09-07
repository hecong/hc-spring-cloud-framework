package com.hc.framework.common.util;

import com.hc.framework.common.constant.SystemConstants;
import lombok.Data;

/**
 * 分页参数工具类
 *
 * <p>统一封装分页查询的输入参数（页码、每页条数）和便捷的计算方法，
 * 适配 MyBatis-Plus、原生 SQL 等不同分页实现场景。</p>
 *
 * <p>防护说明：</p>
 * <ul>
 *     <li>页码最小值为 1，非法值自动修正</li>
 *     <li>每页条数最小值为 1，最大值由 {@link SystemConstants#MAX_PAGE_SIZE} 控制（默认 1000），防止全表查询</li>
 * </ul>
 *
 * <p>原生 SQL 过渡期典型用法（offset / 总页数计算）：</p>
 * <pre>{@code
 * PageUtils page = PageUtils.of(2, 10);
 * page.getOffset();                     // 10（SQL LIMIT offset 值）
 * long totalPages = PageUtils.calcTotalPages(101L, 10);  // 11
 * }</pre>
 *
 * <p>统一分页 API（1.1.0 起，推荐）：</p>
 * <pre>{@code
 * // 入参契约：PageParam（配合 @Valid 校验 pageNum >= 1、1 <= pageSize <= 1000）
 * // 查询转换：param.toPage() → MyBatis-Plus 的 Page/IPage
 * // 返回构造：PageData.of(iPage)（含 list/total/pageNum/pageSize/totalPage/hasNext）
 * }</pre>
 *
 * @deprecated 自 1.1.0 起弃用，计划保留 2 个大版本后（预计 ≥ 3.0）删除。
 * 替代 API：分页入参使用 hc-mybatis-plus 的 PageParam（配合 {@code @Valid} 校验后调用 {@code toPage()}），
 * 分页结果统一使用 PageData.of(IPage) 构建。弃用期内本类行为保持不变
 * （页码修正 / offset / 总页数计算），原生 SQL 过渡可继续使用；
 * 迁移对照与删除排期见 hc-mybatis-plus-spring-boot-starter README「分页 API 收敛」。
 *
 * @author hc-framework
 * @since 1.0.0（1.1.0 起弃用）
 */
@Deprecated
@Data
public class PageUtils {

    /**
     * 当前页码（从 1 开始）
     */
    private int pageNum;

    /**
     * 每页条数
     */
    private int pageSize;

    private PageUtils(int pageNum, int pageSize) {
        this.pageNum = normalizePageNum(pageNum);
        this.pageSize = normalizePageSize(pageSize);
    }

    // ==================== 实例访问器 ====================

    /**
     * 获取当前页码
     *
     * @return 页码（从 1 开始）
     * @deprecated 自 1.1.0 起弃用，请使用 PageParam
     */
    @Deprecated
    public int getPageNum() {
        return pageNum;
    }

    /**
     * 获取每页条数
     *
     * @return 每页条数
     * @deprecated 自 1.1.0 起弃用，请使用 PageParam
     */
    @Deprecated
    public int getPageSize() {
        return pageSize;
    }

    // ==================== 工厂方法 ====================

    /**
     * 构建分页参数
     *
     * @param pageNum  页码（从 1 开始，非法值自动修正为 1）
     * @param pageSize 每页条数（最小 1，最大 {@link SystemConstants#MAX_PAGE_SIZE}）
     * @return 分页参数
     */
    @Deprecated
    public static PageUtils of(int pageNum, int pageSize) {
        return new PageUtils(pageNum, pageSize);
    }

    /**
     * 构建默认分页参数（第 1 页，每页 10 条）
     *
     * @return 默认分页参数
     */
    @Deprecated
    public static PageUtils defaultPage() {
        return new PageUtils(SystemConstants.DEFAULT_PAGE_NUM, SystemConstants.DEFAULT_PAGE_SIZE);
    }

    // ==================== 分页计算 ====================

    /**
     * 获取 SQL LIMIT 的偏移量（offset = (pageNum - 1) * pageSize）
     *
     * <pre>{@code
     * // 第 1 页，每页 10 条 → offset = 0
     * // 第 2 页，每页 10 条 → offset = 10
     * SELECT * FROM user LIMIT #{offset}, #{pageSize}
     * }</pre>
     *
     * @return SQL 偏移量
     */
    @Deprecated
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

    /**
     * 根据总记录数计算总页数
     *
     * @param total 总记录数
     * @return 总页数，total 为 0 时返回 0
     */
    @Deprecated
    public long calcTotalPages(long total) {
        return calcTotalPages(total, pageSize);
    }

    /**
     * 根据总记录数和每页条数计算总页数（静态方法）
     *
     * <pre>{@code
     * PageUtils.calcTotalPages(100L, 10);  // 10
     * PageUtils.calcTotalPages(101L, 10);  // 11
     * PageUtils.calcTotalPages(0L, 10);    // 0
     * }</pre>
     *
     * @param total    总记录数
     * @param pageSize 每页条数
     * @return 总页数
     */
    @Deprecated
    public static long calcTotalPages(long total, int pageSize) {
        if (total <= 0) {
            return 0L;
        }
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * 判断当前页是否是第一页
     *
     * @return true 表示第一页
     */
    @Deprecated
    public boolean isFirstPage() {
        return pageNum == 1;
    }

    /**
     * 判断当前页是否是最后一页
     *
     * @param total 总记录数
     * @return true 表示最后一页
     */
    @Deprecated
    public boolean isLastPage(long total) {
        return pageNum >= calcTotalPages(total);
    }

    // ==================== 参数修正 ====================

    /**
     * 修正页码：最小值为 1
     *
     * @param pageNum 原始页码
     * @return 修正后的页码
     */
    private static int normalizePageNum(int pageNum) {
        return Math.max(pageNum, 1);
    }

    /**
     * 修正每页条数：最小为 1，最大为 {@link SystemConstants#MAX_PAGE_SIZE}
     *
     * @param pageSize 原始每页条数
     * @return 修正后的每页条数
     */
    private static int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), SystemConstants.MAX_PAGE_SIZE);
    }

    @Override
    public String toString() {
        return "PageUtils{pageNum=" + pageNum + ", pageSize=" + pageSize + ", offset=" + getOffset() + "}";
    }
}
