package com.hc.framework.web.model;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Web 模块统一响应结果
 *
 * <p>继承自 {@link com.hc.framework.common.model.Result}，保持与框架其他模块的兼容性。</p>
 * <p>动态字段名配置由 {@link com.hc.framework.web.serializer.ResultSerializer} 处理，
 * 在 Jackson 序列化时动态替换字段名。</p>
 *
 * @param <T> 数据类型
 * @author hc-framework
 * @since 1.0.0
 */
public class Result<T> extends com.hc.framework.common.model.Result<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    public Result() {
        super();
    }

    public Result(Integer code, String message, T data) {
        super(code, message, data);
    }

    // ---- 静态工厂方法（覆盖父类方法，返回 Web 模块的 Result 类型）----

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(HttpStatus.OK.value());
        r.setMessage("操作成功");
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(HttpStatus.OK.value());
        r.setMessage("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(HttpStatus.OK.value());
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error() {
        Result<T> r = new Result<>();
        r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        r.setMessage("操作失败");
        return r;
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    @Override
    public boolean isSuccess() {
        return HttpStatus.OK.value() == this.getCode();
    }
}
