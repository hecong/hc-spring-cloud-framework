package com.hnhegui.framework.excel.model.multisheet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据验证结果
 * 用于在多Sheet导入中对单条数据进行验证
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult<T> {

    /**
     * 是否验证通过
     */
    private boolean valid;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 原始数据
     */
    private T data;

    /**
     * 创建成功结果
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 验证结果
     */
    public static <T> ValidationResult<T> ok(T data) {
        return new ValidationResult<>(true, null, data);
    }

    /**
     * 创建失败结果
     *
     * @param errorMsg 错误信息
     * @param data     数据
     * @param <T>      数据类型
     * @return 验证结果
     */
    public static <T> ValidationResult<T> fail(String errorMsg, T data) {
        return new ValidationResult<>(false, errorMsg, data);
    }

    /**
     * 创建失败结果（无数据）
     *
     * @param errorMsg 错误信息
     * @param <T>      数据类型
     * @return 验证结果
     */
    public static <T> ValidationResult<T> fail(String errorMsg) {
        return new ValidationResult<>(false, errorMsg, null);
    }
}
