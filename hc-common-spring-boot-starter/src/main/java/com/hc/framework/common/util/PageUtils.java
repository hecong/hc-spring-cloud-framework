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
 * <p>典型用法：</p>
 * <pre>{@code
 * // 构建分页参数
 * PageUtils page = PageUtils.of(1, 10);
 * page.getOffset();          // 0（SQL LIMIT offset 值）
 * page.getPageNum();         // 1
 * page.getPageSize();        // 10
 *
 * // 与 MyBatis-Plus 集成
 * Page<User> mpPage = page.toMpPage();
 * IPage<User> result = userMapper.selectPage(mpPage, wrapper);
 *
 * // 计算总页数
 * long totalPages = PageUtils.calcTotalPages(100L, 10);  // 10
 * long totalPages = PageUtils.calcTotalPages(101L, 10);  // 11
 *
 * // 构建默认分页（第1页，每页10条）
 * PageUtils page = PageUtils.defaultPage();
 * }</pre>
 *
 * @author hc-framework
 */
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

    // ==================== 工厂方法 ====================

    /**
     * 构建分页参数
     *
     * @param pageNum  页码（从 1 开始，非法值自动修正为 1）
     * @param pageSize 每页条数（最小 1，最大 {@link SystemConstants#MAX_PAGE_SIZE}）
     * @return 分页参数
     */
    public static PageUtils of(int pageNum, int pageSize) {
        return new PageUtils(pageNum, pageSize);
    }

    /**
     * 构建默认分页参数（第 1 页，每页 10 条）
     *
     * @return 默认分页参数
     */
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
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

    /**
     * 根据总记录数计算总页数
     *
     * @param total 总记录数
     * @return 总页数，total 为 0 时返回 0
     */
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
    public boolean isFirstPage() {
        return pageNum == 1;
    }

    /**
     * 判断当前页是否是最后一页
     *
     * @param total 总记录数
     * @return true 表示最后一页
     */
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
