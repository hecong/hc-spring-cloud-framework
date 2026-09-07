package com.hc.framework.oss.support;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * OSS 上传测试共享工具：追踪流（记录被消费字节数/关闭状态）+ 各类文件样本字节
 */
public final class OssTestSupport {

    private OssTestSupport() {
    }

    /** 最小合法 JPEG：FF D8 FF + 后续负载 */
    public static byte[] jpeg() {
        return concat(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10},
                "JFIF".getBytes(StandardCharsets.ISO_8859_1), repeat(0x42, 20));
    }

    /** PNG 8 字节签名 + 负载 */
    public static byte[] png() {
        byte[] sig = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        return concat(sig, repeat(0x43, 16));
    }

    /** PDF：%PDF- 开头 */
    public static byte[] pdf(int totalLength) {
        return concat("%PDF-1.7\n".getBytes(StandardCharsets.ISO_8859_1), repeat(0x44, totalLength - 8));
    }

    /** ZIP：PK\x03\x04 开头，总长 totalLength */
    public static byte[] zip(int totalLength) {
        byte[] head = {'P', 'K', 0x03, 0x04};
        return concat(head, repeat(0x45, totalLength - 4));
    }

    /** 纯文本负载（无既定魔数） */
    public static byte[] text(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /** 模拟 SDK 读完整流；读流过程抛 IOException 时包装为 RuntimeException（贴近 SDK 行为） */
    public static byte[] consumeAll(InputStream in) {
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("模拟SDK上传读流失败", e);
        }
    }

    /**
     * 追踪输入流：包装内存数据，记录被消费字节数与是否被关闭
     */
    public static final class TrackingInputStream extends InputStream {

        private final ByteArrayInputStream delegate;
        private long consumed;
        private boolean closed;

        public TrackingInputStream(byte[] data) {
            this.delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() {
            int b = delegate.read();
            if (b >= 0) {
                consumed++;
            }
            return b;
        }

        @Override
        public int read(byte @NonNull [] b, int off, int len) {
            int n = delegate.read(b, off, len);
            if (n > 0) {
                consumed += n;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }

        public long consumed() {
            return consumed;
        }

        public boolean closed() {
            return closed;
        }
    }

    private static byte[] concat(byte[] first, byte[]... rest) {
        int total = first.length;
        for (byte[] part : rest) {
            total += part.length;
        }
        byte[] result = new byte[total];
        System.arraycopy(first, 0, result, 0, first.length);
        int offset = first.length;
        for (byte[] part : rest) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static byte[] repeat(int value, int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }
}
