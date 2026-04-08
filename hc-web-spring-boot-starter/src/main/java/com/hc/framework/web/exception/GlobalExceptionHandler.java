package com.hc.framework.web.exception;

import com.hc.framework.web.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {}", e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 参数校验异常 - @Valid/@Validated 方法参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), message);
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), message);
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 参数校验异常 - @RequestParam 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), message);
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = String.format("缺少必要参数: %s", e.getParameterName());
        log.warn(message);
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), message);
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数类型不匹配: %s", e.getName());
        log.warn(message);
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), message);
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {}", e.getMessage());
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.warn("非法状态: {}", e.getMessage());
        Result<Void> result = Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常", e);
        Result<Void> result = Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");
        result.setPath(request.getRequestURI());
        return result;
    }

    /**
     * 其他所有异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        Result<Void> result = Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");
        result.setPath(request.getRequestURI());
        return result;
    }
}
