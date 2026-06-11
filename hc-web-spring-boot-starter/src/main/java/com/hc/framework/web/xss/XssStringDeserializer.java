package com.hc.framework.web.xss;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Jackson 3.x String 类型 XSS 过滤反序列化器
 *
 * <p>在 JSON 反序列化时自动对 String 字段进行 XSS 过滤。</p>
 * <p>使用 {@link XssKit#escape(String)} 进行智能过滤，只移除危险脚本，保留正常字符。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class XssStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getValueAsString();

        if (value == null || value.isEmpty()) {
            return value;
        }

        return XssKit.escape(value);
    }
}
