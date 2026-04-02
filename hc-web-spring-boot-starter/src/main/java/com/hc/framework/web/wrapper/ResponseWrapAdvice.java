package com.hc.framework.web.wrapper;

import com.hc.framework.web.config.WebProperties;
import com.hc.framework.web.model.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.annotation.Resource;

/**
 * 响应自动包装处理器：根据 wrapResponse 开关决定是否包装为 Result
 *
 * <p>统一返回 Result 包装对象，由 Jackson 进行序列化。</p>
 * <p>注意：String 类型不再特殊处理，统一包装为 Result 对象后由 Jackson 序列化，
 * 避免手动拼接 JSON 导致的特殊字符转义问题和 data 层 JSON 二次转义问题。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@RestControllerAdvice
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    @Resource
    private WebProperties webProperties;

    /**
     * 判断是否需要包装：仅当 wrapResponse=true 且返回值不是 Result 时才包装
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return webProperties.getWrapResponse()
            && !returnType.getParameterType().isAssignableFrom(Result.class);
    }

    /**
     * 执行包装：将原始返回值封装为 Result.success(data)
     *
     * <p>统一返回 Result 对象，由 Jackson 进行 JSON 序列化。</p>
     * <p>对于 String 类型，如果当前使用的是 StringHttpMessageConverter（直接返回文本），
     * 则跳过包装，避免破坏原始文本响应（如返回 HTML、纯文本等场景）。</p>
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 如果当前使用的是 StringHttpMessageConverter，说明是文本/plain响应，跳过包装
        // 这通常用于返回 HTML、XML 或其他非 JSON 文本内容的场景
        if (selectedConverterType == StringHttpMessageConverter.class) {
            return body;
        }

        // 统一包装为 Result 对象，由 Jackson 进行序列化
        // 不再对 String 类型特殊处理，避免手动 toString() 导致的问题：
        // 1. 特殊字符（如双引号、换行符）未转义
        // 2. data 层 JSON 被二次转义
        return Result.success(body);
    }
}