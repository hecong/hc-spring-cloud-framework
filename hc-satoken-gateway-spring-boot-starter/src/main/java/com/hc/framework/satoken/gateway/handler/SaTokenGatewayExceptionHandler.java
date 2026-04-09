package com.hc.framework.satoken.gateway.handler;

import cn.dev33.satoken.exception.*;
import com.hc.framework.satoken.gateway.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Sa-Token 网关全局异常处理器
 *
 * <p>统一处理 Sa-Token 相关异常，转换为框架统一的 Result 响应格式。</p>
 * <p>作为 SaReactorFilter.setError() 的兜底处理，处理未被 Filter 捕获的异常。</p>
 *
 * <p>异常码映射：</p>
 * <ul>
 *     <li>未登录 / Token 无效 → 401</li>
 *     <li>无权限 → 403</li>
 *     <li>账号被封禁 → 423</li>
 *     <li>其他认证异常 → 500</li>
 * </ul>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@Order(-1)  // 高优先级，确保在默认异常处理器之前执行
public class SaTokenGatewayExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 获取实际的 Sa-Token 异常（处理被包装的情况）
        SaTokenException saTokenException = extractSaTokenException(ex);
        if (saTokenException == null) {
            return Mono.error(ex);
        }

        log.warn("Sa-Token 网关异常: {}", saTokenException.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 使用共享的 ErrorBuilder 构建响应
        int code = SaTokenGatewayErrorBuilder.getErrorCode(saTokenException);
        String json = SaTokenGatewayErrorBuilder.buildErrorJson(saTokenException);

        // 设置 HTTP 状态码
        response.setStatusCode(HttpStatus.valueOf(code));

        // 写入响应
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * 从异常中提取 Sa-Token 异常
     *
     * <p>处理异常被 RuntimeException 包装的情况。</p>
     *
     * @param ex 原始异常
     * @return SaTokenException，如果不是 Sa-Token 异常则返回 null
     */
    private SaTokenException extractSaTokenException(Throwable ex) {
        // 直接是 SaTokenException
        if (ex instanceof SaTokenException) {
            return (SaTokenException) ex;
        }
        // 检查 cause 链
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SaTokenException) {
                return (SaTokenException) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }
}
