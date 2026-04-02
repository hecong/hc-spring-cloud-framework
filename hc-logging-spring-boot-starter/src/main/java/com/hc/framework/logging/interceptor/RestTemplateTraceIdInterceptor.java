package com.hc.framework.logging.interceptor;

import com.hc.framework.logging.util.TraceIdUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate 请求拦截器：传递 TraceId 实现跨服务链路追踪
 *
 * <p>功能说明：</p>
 * <ul>
 *     <li>在使用 RestTemplate 发起 HTTP 调用时，自动将当前线程的 TraceId 添加到请求头中</li>
 *     <li>接收方服务通过 {@link TraceIdInterceptor} 从请求头中提取 TraceId</li>
 *     <li>实现微服务架构下的全链路日志追踪</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <p>方式一：自动配置（推荐）</p>
 * <p>该拦截器已由 {@code LoggingAutoConfiguration} 自动注册到所有 RestTemplate Bean 中，无需手动配置。</p>
 *
 * <p>方式二：手动配置</p>
 * <pre>{@code
 * @Bean
 * public RestTemplate restTemplate() {
 *     RestTemplate restTemplate = new RestTemplate();
 *     restTemplate.setInterceptors(Collections.singletonList(new RestTemplateTraceIdInterceptor()));
 *     return restTemplate;
 * }
 * }</pre>
 *
 * <p>方式三：针对特定 RestTemplate 配置</p>
 * <pre>{@code
 * restTemplate.getInterceptors().add(new RestTemplateTraceIdInterceptor());
 * }</pre>
 *
 * @author hc-framework
 * @see TraceIdInterceptor
 * @see TraceIdUtils
 */
public class RestTemplateTraceIdInterceptor implements ClientHttpRequestInterceptor {

    /**
     * TraceId 请求头名称
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 拦截 RestTemplate 请求，添加 TraceId 请求头
     *
     * @param request   HTTP 请求
     * @param body      请求体
     * @param execution 请求执行器
     * @return HTTP 响应
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String traceId = TraceIdUtils.getTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            request.getHeaders().add(TRACE_ID_HEADER, traceId);
        }
        return execution.execute(request, body);
    }
}
