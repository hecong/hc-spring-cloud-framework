package com.hnhegui.framework.excel.model;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.util.Map;

/**
 * 模板导出请求
 */
@Data
@Builder
public class TemplateExportRequest {

    /**
     * 模板输入流
     */
    private InputStream templateStream;

    /**
     * 模板文件路径（与templateStream二选一）
     */
    private String templatePath;

    /**
     * Sheet索引（从0开始，默认0）
     */
    private Integer sheetIndex;

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 文件名（不含扩展名）
     */
    private String fileName;

    /**
     * 单行数据（用于简单模板填充）
     */
    private Map<String, Object> data;

    /**
     * 列表数据（用于列表模板填充）
     */
    private Map<String, Object> listData;

    /**
     * 列表数据填充起始行（默认从模板最后一行开始）
     */
    private Integer listStartRow;
}
