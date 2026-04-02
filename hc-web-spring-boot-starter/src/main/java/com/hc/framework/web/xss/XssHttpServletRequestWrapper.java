package com.hc.framework.web.xss;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    // ============================ parameter ============================


    @Override
    public String getParameter(String name) {
        String val = super.getParameter(name);
        return XssKit.escape(val);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;

        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = XssKit.escape(values[i]);
        }
        return result;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = new LinkedHashMap<>();
        Map<String, String[]> parameters = super.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String[] values = entry.getValue();
            for (int i = 0; i < values.length; i++) {
                values[i] = XssKit.escape(values[i]);
            }
            map.put(entry.getKey(), values);
        }
        return map;
    }

    // ============================ attribute ============================
    @Override
    public Object getAttribute(String name) {
        Object value = super.getAttribute(name);
        if (value instanceof String) {
            return XssKit.escape((String) value);
        }
        return value;
    }

    // ============================ header ============================
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null) {
            return null;
        }
        return XssKit.escape(value);
    }

    // ============================ queryString ============================
    @Override
    public String getQueryString() {
        String value = super.getQueryString();
        if (value == null) {
            return null;
        }
        return XssKit.escape(value);
    }
}