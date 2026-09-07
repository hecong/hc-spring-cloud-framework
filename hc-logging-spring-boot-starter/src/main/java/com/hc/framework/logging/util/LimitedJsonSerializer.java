package com.hc.framework.logging.util;

import org.dromara.hutool.json.InternalJSONUtil;
import org.dromara.hutool.json.JSONUtil;
import org.jspecify.annotations.NonNull;

import java.io.Writer;

/**
 * 限长 JSON 序列化：写满 {@code maxLength} 立即中止（不等全量物化再事后截断）
 *
 * <p>原理：hutool6 提供 {@link JSONUtil#toJsonStr(Object, Writer)} 面向 Writer 的流式写出，
 * 底层 Writer 累计超限即抛内部截断信号；序列化被提前终止。超限场景仅返回截断标记
 * {@code <truncated, len=N>}，不会在日志中输出超限对象的完整内容。</p>
 */
public final class LimitedJsonSerializer {

    private LimitedJsonSerializer() {
    }

    /**
     * 序列化为 JSON，超过 maxLength 字符时返回截断标记
     *
     * @param value     待序列化对象（null 返回 "null"）
     * @param maxLength 长度上限（字符），<=0 直接截断
     */
    public static String toLimitedJson(Object value, int maxLength) {
        if (value == null) {
            return "null";
        }
        if (maxLength <= 0) {
            return truncatedMarker(0);
        }
        CountingWriter writer = new CountingWriter(maxLength);
        try {
            writeValue(value, writer);
        } catch (TruncationException e) {
            return truncatedMarker(e.getWritten());
        } catch (RuntimeException e) {
            // 兜底：若 hutool 将截断信号包装，解包 cause 链
            Throwable cause = e.getCause();
            while (cause != null && !(cause instanceof TruncationException)) {
                cause = cause.getCause();
            }
            if (cause != null) {
                return truncatedMarker(((TruncationException) cause).getWritten());
            }
            throw e;
        }
        return writer.toString();
    }

    private static String truncatedMarker(int written) {
        return "<truncated, len=" + written + ">";
    }

    /**
     * 分类型写出：标量直接写（避免 hutool 将字符串按 JSON 文本解析），结构对象走流式序列化
     */
    private static void writeValue(Object value, CountingWriter writer) {
        if (value instanceof CharSequence || value instanceof Character) {
            // 转义 + 引号后流式写出，超长单值同样在超限点立即中止
            InternalJSONUtil.quote(String.valueOf(value), writer);
        } else if (value instanceof Number || value instanceof Boolean) {
            String raw = String.valueOf(value);
            writer.write(raw.toCharArray(), 0, raw.length());
        } else {
            JSONUtil.toJsonStr(value, writer);
        }
    }

    /**
     * 内部截断信号
     */
    private static final class TruncationException extends RuntimeException {
        private final int written;

        TruncationException(int written) {
            super("JSON serialization exceeded limit: " + written);
            this.written = written;
        }

        int getWritten() {
            return written;
        }
    }

    /**
     * 只计数不保留超限内容的自定义 Writer（避免全量物化）
     */
    private static final class CountingWriter extends Writer {

        private final int maxLength;
        private final StringBuilder buffer = new StringBuilder(256);
        private int count;

        CountingWriter(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void write(char @NonNull [] cbuf, int off, int len) {
            if (count + len > maxLength) {
                // 超过上限即停：不保留本次超限片段，抛出截断信号
                throw new TruncationException(maxLength);
            }
            buffer.append(cbuf, off, len);
            count += len;
        }

        @Override
        public void flush() {
            // 无缓冲，无需处理
        }

        @Override
        public void close() {
            // 只作为 JSON 写出目标，不关闭任何外部资源
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }
}
