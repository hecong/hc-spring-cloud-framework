package com.hc.framework.web.wrapper;

import com.hc.framework.web.util.ServletUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求包装器：缓存请求体支持重复读取，并对缓存体大小设置上限，防止大请求导致 OOM。
 *
 * <p>缓存语义（自 1.1.0）：</p>
 * <ul>
 *     <li>Content-Length 明确且 &gt; 上限：不读流、不缓存（{@link #getBodyBytes()} 返回 null）；</li>
 *     <li>Content-Length 缺失/被欺骗：读入后二次防护，实际长度 &gt; 上限则丢弃缓存；</li>
 *     <li>未缓存时 {@link #getInputStream()} 直接返回原始请求流，重复读取能力降级。</li>
 * </ul>
 *
 * @author hecong
 * @since 2026/4/2 14:41
 */
public class CustomizeRequestWrapper extends HttpServletRequestWrapper {

    /** 默认缓存体大小上限：2MB */
    public static final long DEFAULT_MAX_CACHED_BODY = 2L * 1024 * 1024;

    /**
     * 缓存的内容；null 表示超限降级不缓存
     */
    private final byte[] body;

    private final Map<String, String> headerMap = new HashMap<>();

    public CustomizeRequestWrapper(HttpServletRequest request) {
        this(request, DEFAULT_MAX_CACHED_BODY);
    }

    public CustomizeRequestWrapper(HttpServletRequest request, long maxCachedBody) {
        super(request);
        initHeader(request);
        // 已知大小超限：不读流直接降级不缓存（避免大请求 OOM）
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxCachedBody) {
            body = null;
            return;
        }
        byte[] bytes = ServletUtils.getBodyBytes(request);
        // 二次防护：Content-Length 缺失/被欺骗时，读入后发现超限则丢弃缓存
        body = (bytes != null && bytes.length > maxCachedBody) ? null : bytes;
    }

    private void initHeader(HttpServletRequest request) {
        // 初始化 header，统一转小写存储（HTTP header 大小写不敏感）
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headerMap.put(name.toLowerCase(), request.getHeader(name));
        }
    }

    public void addHeader(String name, String value) {
        if (name == null || name.isEmpty()) {
            return;
        }
        headerMap.put(name.toLowerCase(), value);
    }

    public void removeHeader(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        headerMap.remove(name.toLowerCase());
    }

    /**
     * 获取缓存的请求体字节；超限降级（不缓存）时返回 null
     */
    public byte[] getBodyBytes() {
        return body;
    }

    @Override
    public String getHeader(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return headerMap.get(name.toLowerCase());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headerMap.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String lowerCase = name.toLowerCase();
        List<String> values = new ArrayList<>();
        if (headerMap.containsKey(lowerCase)) {
            values.add(headerMap.get(lowerCase));
        }
        return Collections.enumeration(values);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 超限降级：直接消费原始请求流（缓存不可用时重复读取能力降级）
        if (body == null) {
            return getRequest().getInputStream();
        }
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        // 返回 ServletInputStream
        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int available() {
                return body.length;
            }

        };
    }

}
