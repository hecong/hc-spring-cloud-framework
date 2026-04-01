package com.hnhegui.framework.excel.model.multisheet;

import com.alibaba.excel.read.listener.ReadListener;
import com.hnhegui.framework.excel.service.ExcelImportService;
import lombok.Getter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 多Sheet导入构建器
 * 支持链式配置多个Sheet的导入参数
 *
 * <p>使用示例：</p>
 * <pre>
 * MultiSheetImportResult result = excelImportService.importMultiSheet(file.getInputStream())
 *     .sheet(0, "用户基础", UserBasicDTO.class)
 *         .batchSize(100)
 *         .validator(data -> {
 *             if (StringUtils.isBlank(data.getName())) {
 *                 return ValidationResult.fail("姓名不能为空", data);
 *             }
 *             return ValidationResult.ok(data);
 *         })
 *         .batchHandler(dataList -> userService.saveBasic(dataList))
 *     .sheet(1, "用户地址", UserAddressDTO.class)
 *         .batchHandler(dataList -> userService.saveAddress(dataList))
 *     .doRead();
 * </pre>
 */
public class MultiSheetImportBuilder {

    /**
     * 文件输入流
     * -- GETTER --
     *  获取文件输入流
     *
     * @return 文件输入流

     */
    @Getter
    private final InputStream inputStream;

    /**
     * Excel导入服务
     */
    private final ExcelImportService importService;

    /**
     * Sheet配置列表
     */
    @Getter
    private final List<SheetConfig<?>> sheetConfigs = new ArrayList<>();

    /**
     * 当前正在配置的Sheet
     */
    private SheetConfig<?> currentSheet;

    /**
     * 多Sheet上下文
     */
    @Getter
    private final MultiSheetContext context = new MultiSheetContext();

    /**
     * 构造函数
     *
     * @param inputStream  文件输入流
     * @param importService Excel导入服务
     */
    public MultiSheetImportBuilder(InputStream inputStream, ExcelImportService importService) {
        this.inputStream = inputStream;
        this.importService = importService;
    }

    /**
     * 添加Sheet配置
     *
     * @param sheetIndex Sheet索引（从0开始）
     * @param sheetName  Sheet名称
     * @param clazz      数据类型
     * @param <T>        数据类型
     * @return Sheet配置（支持链式调用）
     */
    public <T> SheetConfig<T> sheet(int sheetIndex, String sheetName, Class<T> clazz) {
        SheetConfig<T> config = new SheetConfig<>(sheetIndex, sheetName, clazz);
        config.setBuilder(this);  // 设置构建器引用
        sheetConfigs.add(config);
        currentSheet = config;
        return config;
    }

    /**
     * 添加Sheet配置（使用索引）
     *
     * @param sheetIndex Sheet索引（从0开始）
     * @param clazz      数据类型
     * @param <T>        数据类型
     * @return Sheet配置
     */
    public <T> SheetConfig<T> sheet(int sheetIndex, Class<T> clazz) {
        return sheet(sheetIndex, "Sheet" + (sheetIndex + 1), clazz);
    }

    /**
     * 添加Sheet配置（使用名称）
     *
     * @param sheetName Sheet名称
     * @param clazz     数据类型
     * @param <T>       数据类型
     * @return Sheet配置
     */
    public <T> SheetConfig<T> sheet(String sheetName, Class<T> clazz) {
        // 名称模式时，索引设为-1，后续通过名称查找
        return sheet(-1, sheetName, clazz);
    }

    /**
     * 执行导入
     *
     * @return 多Sheet导入结果
     */
    public MultiSheetImportResult doRead() {
        return importService.importMultiSheet(inputStream, this);
    }

    /**
     * 设置批次大小（当前Sheet）
     *
     * @param batchSize 批次大小
     * @return 当前构建器
     */
    public MultiSheetImportBuilder batchSize(int batchSize) {
        if (currentSheet != null) {
            currentSheet.setBatchSize(batchSize);
        }
        return this;
    }

    /**
     * 设置验证器（当前Sheet）
     *
     * @param validator 验证器
     * @param <T>       数据类型
     * @return 当前构建器
     */
    @SuppressWarnings("unchecked")
    public <T> MultiSheetImportBuilder validator(Function<T, ValidationResult<T>> validator) {
        if (currentSheet != null) {
            ((SheetConfig<T>) currentSheet).setValidator(validator);
        }
        return this;
    }

    /**
     * 设置验证器（带上下文，当前Sheet）
     *
     * @param validator 验证器
     * @param <T>       数据类型
     * @return 当前构建器
     */
    @SuppressWarnings("unchecked")
    public <T> MultiSheetImportBuilder validator(BiFunction<T, MultiSheetContext, ValidationResult<T>> validator) {
        if (currentSheet != null) {
            ((SheetConfig<T>) currentSheet).setValidatorWithContext(validator);
        }
        return this;
    }

    /**
     * 设置批次处理器（当前Sheet）
     *
     * @param batchHandler 批次处理器
     * @param <T>          数据类型
     * @return 当前构建器
     */
    @SuppressWarnings("unchecked")
    public <T> MultiSheetImportBuilder batchHandler(Consumer<List<T>> batchHandler) {
        if (currentSheet != null) {
            ((SheetConfig<T>) currentSheet).setBatchHandler(batchHandler);
        }
        return this;
    }

    /**
     * 设置错误处理器（当前Sheet）
     *
     * @param errorHandler 错误处理器
     * @return 当前构建器
     */
    public MultiSheetImportBuilder errorHandler(Consumer<SheetError> errorHandler) {
        if (currentSheet != null) {
            currentSheet.setErrorHandler(errorHandler);
        }
        return this;
    }

    /**
     * 设置头部行数（当前Sheet）
     *
     * @param headRowNumber 头部行数
     * @return 当前构建器
     */
    public MultiSheetImportBuilder headRowNumber(int headRowNumber) {
        if (currentSheet != null) {
            currentSheet.setHeadRowNumber(headRowNumber);
        }
        return this;
    }

    /**
     * 设置是否必填（当前Sheet）
     *
     * @param required 是否必填
     * @return 当前构建器
     */
    public MultiSheetImportBuilder required(boolean required) {
        if (currentSheet != null) {
            currentSheet.setRequired(required);
        }
        return this;
    }

    /**
     * 设置必填（当前Sheet）
     *
     * @return 当前构建器
     */
    public MultiSheetImportBuilder required() {
        return required(true);
    }

}
