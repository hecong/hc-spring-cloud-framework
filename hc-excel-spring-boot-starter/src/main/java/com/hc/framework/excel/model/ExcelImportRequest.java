package com.hc.framework.excel.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel导入请求
 */
@Data
@Builder
public class ExcelImportRequest {

    /**
     * 上传文件
     */
    private MultipartFile file;

    /**
     * Excel表格名称（sheet页名称，默认第一个）
     */
    private String sheetName;

    /**
     * 标题行索引（从0开始，默认0）
     */
    private Integer headRowNumber;

    /**
     * 每批处理数量（默认1000）
     */
    private Integer batchSize;

    /**
     * 是否校验数据（默认true）
     */
    private Boolean validateData;
}
