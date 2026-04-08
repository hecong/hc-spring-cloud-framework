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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author hecong
 * @since 2026/4/2 14:41
 */
public class CustomizeRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存的内容
     */
    private final byte[] body;

    private final Map<String, String> headerMap = new HashMap<>();

    public CustomizeRequestWrapper(HttpServletRequest request) {
        super(request);
        initHeader(request);
        // 初始化 body
        body = ServletUtils.getBodyBytes(request);
    }

    private void initHeader(HttpServletRequest request) {
        // 初始化 header
        Enumeration<String> headerNames = request.getHeaderNames();
        Iterator<String> iterator = headerNames.asIterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            headerMap.put(name, request.getHeader(name));
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