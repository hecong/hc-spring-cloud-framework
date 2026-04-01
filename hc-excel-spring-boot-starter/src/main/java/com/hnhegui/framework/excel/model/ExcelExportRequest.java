package com.hnhegui.framework.excel.model;

import lombok.Builder;
import lombok.Data;

/**
 * Excel导出请求
 */
@Data
@Builder
public class ExcelExportRequest {

    /**
     * 文件名（不含扩展名）
     */
    private String fileName;

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 标题行索引（从0开始，默认0）
     */
    private Integer headRowNumber;

    /**
     * 每批查询数量（默认5000）
     */
    private Integer batchSize;
}
