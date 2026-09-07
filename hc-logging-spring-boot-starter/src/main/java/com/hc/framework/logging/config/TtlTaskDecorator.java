package com.hc.framework.logging.config;

import com.alibaba.ttl.TtlRunnable;
import com.hc.framework.logging.util.TraceIdUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * 基于 TTL 的任务装饰器：提交线程 TraceId 透传到任务线程，任务结束回滚 MDC
 *
 * <p>用法：</p>
 * <ul>
 *     <li>框架自动装配：无自定义 {@link TaskDecorator} 时自动注册并注入默认异步执行器，业务无需改动；</li>
 *     <li>自定义线程池：{@code executor.setTaskDecorator(new TtlTaskDecorator())} 即可接入透传。</li>
 * </ul>
 *
 * <p>TTL 仅采用 API 方式（任务包装）传递，不依赖 {@code -javaagent} 运维参数。</p>
 */
public class TtlTaskDecorator implements TaskDecorator {

    /**
     * 包装任务：捕获提交线程 traceId，任务执行期间恢复 MDC，结束后回滚
     */
    @Override
    public @NonNull Runnable decorate(@NonNull Runnable task) {
        final String traceId = TraceIdUtils.getTraceId();
        // TTL 负责在任务执行线程回放可传递上下文
        Runnable ttlTask = TtlRunnable.get(task);
        if (traceId == null || traceId.isEmpty()) {
            // 提交线程无 traceId：仍回放（值本身为空），不写 MDC
            return ttlTask;
        }
        return () -> {
            try {
                MDC.put(TraceIdUtils.TRACE_ID_KEY, traceId);
                ttlTask.run();
            } finally {
                MDC.remove(TraceIdUtils.TRACE_ID_KEY);
            }
        };
    }
}
