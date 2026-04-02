package com.hc.framework.web.serializer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.hc.framework.web.config.WebProperties;
import com.hc.framework.web.model.Result;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

/**
 * 自定义 Result 序列化器，支持动态字段名
 */
public class ResultObjectWriter implements ObjectWriter<Result<?>> {

    private final WebProperties webProperties;

    public ResultObjectWriter(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (!(object instanceof Result<?> result)) {
            jsonWriter.writeNull();
            return;
        }

        // 获取配置的自定义字段名
        String codeField = webProperties.getCodeField();
        String messageField = webProperties.getMessageField();
        String dataField = webProperties.getDataField();

        // 开始序列化
        jsonWriter.startObject();

        // 序列化 code 字段（动态名）
        jsonWriter.writeName(codeField);
        jsonWriter.writeInt32(result.getCode());

        // 序列化 message 字段（动态名）
        jsonWriter.writeName(messageField);
        jsonWriter.writeString(result.getMessage());

        // 序列化 data 字段（动态名）
        jsonWriter.writeName(dataField);
        if (result.getData() == null) {
            jsonWriter.writeNull();
        } else {
            jsonWriter.writeAny(result.getData());
        }

        // 序列化 timestamp 字段
        jsonWriter.writeName("timestamp");
        LocalDateTime timestamp = result.getTimestamp();
        jsonWriter.writeString(timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 序列化 path 字段
        if (result.getPath() != null) {
            jsonWriter.writeName("path");
            jsonWriter.writeString(result.getPath());
        }

        // 结束序列化
        jsonWriter.endObject();
    }
}