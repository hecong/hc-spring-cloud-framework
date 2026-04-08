package com.hc.framework.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * <p>该类作为数据载体，本身不包含序列化逻辑。</p>
 * <p>动态字段名配置由 {@link com.hc.framework.web.serializer.ResultSerializer} 处理，
 * 在 Jackson 序列化时动态替换字段名。</p>
 *
 * @param <T> 数据类型
 * @author hc-framework
 * @since 1.0.0
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 请求路径
     */
    private String path;

    public Result() {
        this.timestamp = LocalDateTime.now();
    }

    public Result(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(HttpStatus.OK.value(), "操作成功", null);
    }

    /**
     * 创建成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(HttpStatus.OK.value(), "操作成功", data);
    }

    /**
     * 创建成功响应（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(HttpStatus.OK.value(), message, data);
    }

    /**
     * 创建失败响应（默认错误码）
     */
    public static <T> Result<T> error() {
        return new Result<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "操作失败", null);
    }

    /**
     * 创建失败响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    /**
     * 创建失败响应（自定义错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断是否为成功响应
     */
    public boolean isSuccess() {
        return HttpStatus.OK.value() == this.code;
    }
}