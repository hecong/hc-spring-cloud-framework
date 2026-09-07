package com.hc.framework.common.util;

import com.hc.framework.common.constant.SystemConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageUtils 弃用后行为保持验证：
 * 非法值修正、offset、总页数计算与弃用前一致（不得边废弃边改语义）
 */
@SuppressWarnings("deprecation")
class PageUtilsTest {

    // ---------- 非法值修正 ----------

    @Test
    @DisplayName("of(0,0)：页码与每页条数均被修正为 1")
    void ofIllegalValuesNormalized() {
        PageUtils page = PageUtils.of(0, 0);
        assertEquals(1, page.getPageNum());
        assertEquals(1, page.getPageSize());
    }

    @Test
    @DisplayName("of(负数,-1)：页码/每页条数下限修正仍生效")
    void ofNegativeValuesNormalized() {
        PageUtils page = PageUtils.of(-5, -1);
        assertEquals(1, page.getPageNum());
        assertEquals(1, page.getPageSize());
    }

    @Test
    @DisplayName("of(1,5000)：每页条数超出上限被修正为 MAX_PAGE_SIZE")
    void pageSizeCappedAtMax() {
        PageUtils page = PageUtils.of(1, 5000);
        assertEquals(1, page.getPageNum());
        assertEquals(SystemConstants.MAX_PAGE_SIZE, page.getPageSize());
    }

    // ---------- offset 计算 ----------

    @Test
    @DisplayName("getOffset：第 1 页 offset=0，第 2 页 offset=pageSize")
    void offsetCalculation() {
        assertEquals(0L, PageUtils.of(1, 10).getOffset());
        assertEquals(10L, PageUtils.of(2, 10).getOffset());
        assertEquals(100L, PageUtils.of(11, 10).getOffset());
    }

    @Test
    @DisplayName("getOffset：修正后每页 1000 时 offset=(pageNum-1)*1000")
    void offsetWithCappedPageSize() {
        PageUtils page = PageUtils.of(3, 2000);
        assertEquals(2000L, page.getOffset());
    }

    // ---------- 总页数计算 ----------

    @Test
    @DisplayName("calcTotalPages 静态：0/100/101 条按每页 10 条计算")
    void calcTotalPagesStatic() {
        assertEquals(0L, PageUtils.calcTotalPages(0L, 10));
        assertEquals(10L, PageUtils.calcTotalPages(100L, 10));
        assertEquals(11L, PageUtils.calcTotalPages(101L, 10));
    }

    @Test
    @DisplayName("calcTotalPages 静态：负总数按 0 处理")
    void calcTotalPagesNegativeTotal() {
        assertEquals(0L, PageUtils.calcTotalPages(-1L, 10));
    }

    @Test
    @DisplayName("calcTotalPages 实例：使用实例每页条数计算")
    void calcTotalPagesInstance() {
        PageUtils page = PageUtils.of(1, 10);
        assertEquals(10L, page.calcTotalPages(100L));
        assertEquals(11L, page.calcTotalPages(101L));
    }

    // ---------- 默认值与边界状态 ----------

    @Test
    @DisplayName("defaultPage：第 1 页每页 10 条")
    void defaultPage() {
        PageUtils page = PageUtils.defaultPage();
        assertEquals(SystemConstants.DEFAULT_PAGE_NUM, page.getPageNum());
        assertEquals(SystemConstants.DEFAULT_PAGE_SIZE, page.getPageSize());
    }

    @Test
    @DisplayName("isFirstPage / isLastPage 边界判断与弃用前一致")
    void firstAndLastPage() {
        assertTrue(PageUtils.of(1, 10).isFirstPage());
        assertFalse(PageUtils.of(2, 10).isFirstPage());

        // 共 20 条、每页 10 条：第 1 页非末页，第 2 页为末页
        assertFalse(PageUtils.of(1, 10).isLastPage(20L));
        assertTrue(PageUtils.of(2, 10).isLastPage(20L));
        // 无数据时第 1 页也算末页
        assertTrue(PageUtils.of(1, 10).isLastPage(0L));
    }
}
