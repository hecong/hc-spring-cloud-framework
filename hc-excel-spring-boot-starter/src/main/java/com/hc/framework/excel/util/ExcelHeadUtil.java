package com.hc.framework.excel.util;

import com.hc.framework.excel.model.DynamicHead;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel表头工具类
 *
 * <p>提供动态表头和数据行的构建工具方法</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class ExcelHeadUtil {

    private ExcelHeadUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 构建动态表头
     *
     * @param heads 动态表头定义列表
     * @return 表头列表
     */
    public static List<List<String>> buildDynamicHeads(List<DynamicHead> heads) {
        List<List<String>> headList = new ArrayList<>();
        for (DynamicHead head : heads) {
            headList.add(List.of(head.getName()));
        }
        return headList;
    }

    /**
     * 构建动态数据行
     *
     * @param heads    动态表头定义列表
     * @param dataList 数据列表
     * @return 数据行列表
     */
    public static List<List<Object>> buildDynamicRows(List<DynamicHead> heads, List<Map<String, Object>> dataList) {
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> dataMap : dataList) {
            List<Object> row = new ArrayList<>();
            for (DynamicHead head : heads) {
                row.add(dataMap.get(head.getField()));
            }
            rows.add(row);
        }
        return rows;
    }
}
