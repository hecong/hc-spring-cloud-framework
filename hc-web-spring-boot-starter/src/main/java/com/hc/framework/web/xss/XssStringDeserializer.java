package com.hc.framework.web.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Jackson String 类型 XSS 过滤反序列化器
 *
 * <p>在 JSON 反序列化时自动对 String 字段进行 XSS 过滤。</p>
 * <p>使用 {@link XssKit#escape(String)} 进行智能过滤，只移除危险脚本，保留正常字符。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class XssStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isEmpty()) {
            return value;
        }

        // 使用智能过滤，保留正常 HTML 字符
        return XssKit.escape(value);
    }
}