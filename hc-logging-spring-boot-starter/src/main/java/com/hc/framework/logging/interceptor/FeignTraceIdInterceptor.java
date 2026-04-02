package com.hc.framework.logging.interceptor;

import com.hc.framework.common.constant.HttpConstants;
import com.hc.framework.logging.util.TraceIdUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign 请求拦截器：传递 TraceId 实现跨服务链路追踪
 *
 * <p>功能说明：</p>
 * <ul>
 *     <li>在发起 Feign 调用时，自动将当前线程的 TraceId 添加到请求头中</li>
 *     <li>接收方服务通过 {@link TraceIdInterceptor} 从请求头中提取 TraceId</li>
 *     <li>实现微服务架构下的全链路日志追踪</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <p>该拦截器已由 {@code LoggingAutoConfiguration} 自动注册，无需手动配置。</p>
 * <p>如需手动注册，可在 Feign 配置类中：</p>
 * <pre>{@code
 * @Bean
 * public FeignTraceIdInterceptor feignTraceIdInterceptor() {
 *     return new FeignTraceIdInterceptor();
 * }
 * }</pre>
 *
 * @author hc-framework
 * @see TraceIdInterceptor
 * @see TraceIdUtils
 */
public class FeignTraceIdInterceptor implements RequestInterceptor {

    /**
     * TraceId 请求头名称
     */
    private static final String TRACE_ID_HEADER = HttpConstants.HEADER_TRACE_ID;

    /**
     * 拦截 Feign 请求，添加 TraceId 请求头
     *
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        String traceId = TraceIdUtils.getTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TRACE_ID_HEADER, traceId);
        }
    }
}
