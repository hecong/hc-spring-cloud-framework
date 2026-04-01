package com.hnhegui.framework.excel.model.multisheet;

import com.alibaba.excel.read.listener.ReadListener;
import lombok.Data;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Sheet配置类
 * 用于配置多Sheet导入中单个Sheet的读取参数
 *
 * @param <T> 数据类型
 */
@Data
public class SheetConfig<T> {

    /**
     * 默认批次大小
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 当前构建器（用于链式返回）
     * -- SETTER --
     *  设置当前构建器
     *
     */
    private MultiSheetImportBuilder builder;

    /**
     * Sheet索引（从0开始）
     */
    private Integer sheetIndex;

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 数据类型
     */
    private Class<T> clazz;

    /**
     * 批次大小
     */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * 数据验证器（单条验证）
     */
    private Function<T, ValidationResult<T>> validator;

    /**
     * 数据验证器（带上下文）
     */
    private BiFunction<T, MultiSheetContext, ValidationResult<T>> validatorWithContext;

    /**
     * 批次处理回调
     */
    private Consumer<List<T>> batchHandler;

    /**
     * 错误处理回调
     */
    private Consumer<SheetError> errorHandler;

    /**
     * 自定义读取监听器
     */
    private ReadListener<T> readListener;

    /**
     * 头部行数
     */
    private Integer headRowNumber = 1;

    /**
     * 是否必填
     */
    private boolean required = false;

    /**
     * 构造函数
     *
     * @param sheetIndex Sheet索引
     * @param sheetName  Sheet名称
     * @param clazz      数据类型
     */
    public SheetConfig(Integer sheetIndex, String sheetName, Class<T> clazz) {
        this.sheetIndex = sheetIndex;
        this.sheetName = sheetName;
        this.clazz = clazz;
    }

    /**
     * 设置批次大小
     *
     * @param batchSize 批次大小
     * @return 构建器
     */
    public MultiSheetImportBuilder batchSize(int batchSize) {
        this.batchSize = batchSize;
        return builder;
    }

    /**
     * 设置验证器
     *
     * @param validator 验证器
     * @return 构建器
     */
    public MultiSheetImportBuilder validator(Function<T, ValidationResult<T>> validator) {
        this.validator = validator;
        return builder;
    }

    /**
     * 设置验证器（带上下文）
     *
     * @param validator 验证器
     * @return 构建器
     */
    public MultiSheetImportBuilder validator(BiFunction<T, MultiSheetContext, ValidationResult<T>> validator) {
        this.validatorWithContext = validator;
        return builder;
    }

    /**
     * 设置批次处理器
     *
     * @param batchHandler 批次处理器
     * @return 构建器
     */
    public MultiSheetImportBuilder batchHandler(Consumer<List<T>> batchHandler) {
        this.batchHandler = batchHandler;
        return builder;
    }
}
