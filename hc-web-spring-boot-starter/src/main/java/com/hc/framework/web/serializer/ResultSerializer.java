package com.hc.framework.web.serializer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import com.hc.framework.web.config.WebProperties;
import com.hc.framework.web.model.Result;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 3.x 自定义 Result 序列化器
 *
 * <p>支持动态字段名配置，根据 WebProperties 中的配置序列化字段名。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class ResultSerializer extends ValueSerializer<Result<?>> {

    private final WebProperties webProperties;

    public ResultSerializer(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void serialize(Result<?> result, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        String codeField = webProperties.getCodeField();
        String messageField = webProperties.getMessageField();
        String dataField = webProperties.getDataField();

        gen.writeStartObject();

        gen.writeName(codeField);
        gen.writeNumber(result.getCode());

        gen.writeName(messageField);
        gen.writeString(result.getMessage());

        if (result.getData() == null) {
            gen.writeName(dataField);
            gen.writeNull();
        } else {
            gen.writeName(dataField);
            gen.writePOJO(result.getData());
        }

        if (result.getTimestamp() != null) {
            gen.writeName("timestamp");
            gen.writeString(result.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (result.getPath() != null) {
            gen.writeName("path");
            gen.writeString(result.getPath());
        }

        gen.writeEndObject();
    }
}
