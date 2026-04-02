package com.hc.framework.web.xss;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

/**
 * Fastjson2 String 类型 XSS 过滤反序列化器
 *
 * <p>在 JSON 反序列化时自动对 String 字段进行 XSS 过滤。</p>
 * <p>使用 {@link XssKit#escape(String)} 进行智能过滤，只移除危险脚本，保留正常字符。</p>
 *
 * 或在该字段上使用自定义的反序列化器。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class XssStringDeserializer implements ObjectReader<String> {

    public static final XssStringDeserializer INSTANCE = new XssStringDeserializer();

    private XssStringDeserializer() {
        // 单例模式
    }

    /**
     * 读取并过滤 String 值
     *
     * @param jsonReader JSON 读取器
     * @param fieldType  字段类型
     * @param fieldName  字段名称
     * @param features   特性标志
     * @return 过滤后的安全字符串
     */
    @Override
    public String readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
        String value = jsonReader.readString();

        if (value == null || value.isEmpty()) {
            return value;
        }

        // 使用智能过滤，保留正常 HTML 字符
        return XssKit.escape(value);
    }
}