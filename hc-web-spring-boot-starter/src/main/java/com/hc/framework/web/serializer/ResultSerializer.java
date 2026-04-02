package com.hc.framework.web.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hc.framework.web.config.WebProperties;
import com.hc.framework.web.model.Result;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 自定义 Result 序列化器
 *
 * <p>支持动态字段名配置，根据 WebProperties 中的配置序列化字段名。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class ResultSerializer extends JsonSerializer<Result<?>> {

    private final WebProperties webProperties;

    public ResultSerializer(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void serialize(Result<?> result, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 获取配置的字段名
        String codeField = webProperties.getCodeField();
        String messageField = webProperties.getMessageField();
        String dataField = webProperties.getDataField();

        gen.writeStartObject();

        // 序列化 code 字段（动态名）
        gen.writeNumberField(codeField, result.getCode());

        // 序列化 message 字段（动态名）
        gen.writeStringField(messageField, result.getMessage());

        // 序列化 data 字段（动态名）
        if (result.getData() == null) {
            gen.writeNullField(dataField);
        } else {
            gen.writeFieldName(dataField);
            serializers.defaultSerializeValue(result.getData(), gen);
        }

        // 序列化 timestamp 字段
        if (result.getTimestamp() != null) {
            gen.writeStringField("timestamp", 
                result.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // 序列化 path 字段
        if (result.getPath() != null) {
            gen.writeStringField("path", result.getPath());
        }

        gen.writeEndObject();
    }
}
