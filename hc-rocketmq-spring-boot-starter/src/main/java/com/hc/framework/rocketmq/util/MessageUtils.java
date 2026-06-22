package com.hc.framework.rocketmq.util;

import com.hc.framework.common.util.JsonUtils;
import com.hc.framework.rocketmq.core.BaseMqMessage;
import org.apache.rocketmq.client.apis.message.MessageView;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 消息解析工具类（统一 ByteBuffer → String / MessageView → BaseMqMessage 转换）
 *
 * <p>消除 {@link com.hc.framework.rocketmq.core.consumer.BaseMqConsumer} 和
 * {@link com.hc.framework.rocketmq.core.transaction.BaseTransactionChecker}
 * 中重复的 ByteBuffer 解析逻辑。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public final class MessageUtils {

    private MessageUtils() {
    }

    /**
     * ByteBuffer → UTF-8 字符串。
     */
    public static String byteBufferToString(ByteBuffer buffer) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = new byte[buffer.remaining()];
            System.arraycopy(buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    bytes, 0, buffer.remaining());
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.duplicate().get(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * MessageView → BaseMqMessage（便捷方法）。
     */
    public static BaseMqMessage parseMessage(MessageView messageView) {
        String body = byteBufferToString(messageView.getBody());
        return JsonUtils.fromJson(body, BaseMqMessage.class);
    }
}
