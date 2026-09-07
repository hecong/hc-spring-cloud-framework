package com.hc.framework.oss.support;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * OSS 上传统一校验组件（阿里云 / MinIO / 腾讯 COS 共用）。
 *
 * <p>校验链：<b>扩展名白名单 → 文件头魔数（magic number）→ 大小上限</b>。</p>
 * <ul>
 *   <li>扩展名白名单默认覆盖常见图片/文档/文本/压缩/音视频；{@code .jsp} / {@code .exe} / 无后缀默认拒绝。</li>
 *   <li>对具有既定文件签名的类型读取文件头字节比对（防扩展名伪造），比对前通过 {@link PushbackInputStream} 回推，
 *       保证上传 SDK 仍从流的原始头部开始消费；无统一签名的文本类（txt/csv/md）仅做后缀+大小校验。</li>
 *   <li>{@code contentLength == -1}（未知大小）时以限制读取流包装，累计读取超过上限即抛
 *       {@link UploadSizeLimitExceededException} 并停止读取。</li>
 *   <li>校验失败统一抛 {@link IllegalArgumentException}（业务可区分"校验失败"与"上传失败"）。</li>
 * </ul>
 */
@Getter
public class OssUploadValidator {

    /**
     * 默认单文件大小上限：100MB
     */
    public static final long DEFAULT_MAX_FILE_SIZE = 100L * 1024 * 1024;

    /**
     * 默认允许的扩展名白名单
     */
    public static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "md",
        "zip", "rar", "7z",
        "mp3", "mp4"
    );

    /**
     * 文件大小超限标记异常（流式上传过程中由限制读取流抛出，上传层据此解包为 IllegalArgumentException）
     */
    public static final class UploadSizeLimitExceededException extends IOException {

        @Serial
        private static final long serialVersionUID = 1L;

        public UploadSizeLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * 魔数签名：读取 headLength 字节，若总数不足或 Predicate 不满足则判定内容与扩展名不符
     */
    private record MagicSignature(int headLength, Predicate<byte[]> matches) {
    }

    /**
     * 具有既定文件签名的扩展名 → 魔数规则
     */
    private static final Map<String, MagicSignature> MAGIC_SIGNATURES = buildMagicSignatures();

    private final boolean enabled;
    private final Set<String> allowedExtensions;
    private final long maxFileSize;

    public OssUploadValidator() {
        this(true, null, DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * @param enabled           是否启用校验（{@code false} 时跳过全部校验，仅由上传层负责关流）
     * @param allowedExtensions 扩展名白名单（空/空集合时回落默认内置表）；自动转小写并容忍前导点号
     * @param maxFileSize       单文件大小上限（字节，&le;0 时回落默认 100MB）
     */
    public OssUploadValidator(boolean enabled, Collection<String> allowedExtensions, long maxFileSize) {
        this.enabled = enabled;
        this.allowedExtensions = Collections.unmodifiableSet(normalizeExtensions(
            allowedExtensions == null || allowedExtensions.isEmpty()
                ? DEFAULT_ALLOWED_EXTENSIONS : allowedExtensions));
        this.maxFileSize = maxFileSize <= 0 ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
    }

    /**
     * 扩展名 + 已知大小预检（不读取流）。
     *
     * @return 归一化后的扩展名（不含点号，小写）；禁用校验时返回 null
     * @throws IllegalArgumentException 文件名非法 / 扩展名不在白名单 / 声明大小超限
     */
    public String validate(String fileName, long contentLength) {
        if (!enabled) {
            return null;
        }
        return doValidate(fileName, contentLength);
    }

    /**
     * 校验并准备上传流：校验失败抛 {@link IllegalArgumentException}（不读取/已读取并中止）；
     * 通过后返回 SDK 可直接从头消费的包装流（含回推后的原流）。
     *
     * <p>调用方负责在 finally 中关闭<b>原始输入流</b>（包装流关闭会级联关闭底层）。</p>
     */
    public InputStream prepare(String fileName, InputStream input, long contentLength) {
        if (input == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        if (!enabled) {
            return input;
        }
        String ext = doValidate(fileName, contentLength);

        InputStream delegate = input;
        if (contentLength < 0) {
            // 未知大小：限制读取流，累计读超上限即中止
            delegate = new BoundedInputStream(delegate, maxFileSize);
        }

        MagicSignature signature = MAGIC_SIGNATURES.get(ext);
        if (signature == null) {
            // 无既定签名的类型（txt/csv/md 等）仅做后缀+大小校验
            return delegate;
        }

        // 读文件头做魔数比对，比对前回推，保证 SDK 从头消费
        PushbackInputStream pushback = new PushbackInputStream(delegate, signature.headLength());
        byte[] head = new byte[signature.headLength()];
        int total = 0;
        try {
            while (total < head.length) {
                int read = pushback.read(head, total, head.length - total);
                if (read == -1) {
                    break;
                }
                total += read;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件头校验魔数失败: " + fileName, e);
        }
        if (total < signature.headLength() || !signature.matches().test(head)) {
            throw new IllegalArgumentException("文件内容与扩展名不符，疑似伪造类型: ." + ext);
        }
        try {
            pushback.unread(head, 0, total);
        } catch (IOException e) {
            throw new IllegalArgumentException("回推文件头失败: " + fileName, e);
        }
        return pushback;
    }

    /**
     * 判断异常（或其 cause 链）中是否包含指定类型异常。
     * 用于上传层识别 SDK 包装后的"大小超限/读流"标记异常。
     */
    public static boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 静默关闭输入流
     */
    public static void closeQuietly(InputStream input) {
        if (input == null) {
            return;
        }
        try {
            input.close();
        } catch (IOException ignored) {
            // 关闭失败无可恢复路径，忽略
        }
    }

    private String doValidate(String fileName, long contentLength) {
        String ext = extensionOf(fileName);
        if (ext == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (!allowedExtensions.contains(ext)) {
            throw new IllegalArgumentException("不允许的文件类型: " + (ext.isEmpty() ? "<无后缀>" : "." + ext));
        }
        if (contentLength > 0 && contentLength > maxFileSize) {
            throw new IllegalArgumentException(
                "文件大小超出限制: " + contentLength + " 字节 > " + maxFileSize + " 字节");
        }
        return ext;
    }

    /**
     * 取归一化扩展名（小写、不含点）；空/纯点文件名为 null（非法）
     */
    private static String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String name = fileName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeExtensions(Collection<String> extensions) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String ext : extensions) {
            if (ext == null) {
                continue;
            }
            String value = ext.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith(".")) {
                value = value.substring(1);
            }
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    /**
     * 限制读取流：累计读取超过 maxBytes 即抛 {@link UploadSizeLimitExceededException} 并停止。
     * 恰好等于 maxBytes 的文件放行；超过一个字节即拒绝（防止"声明上限恰好被绕过"）。
     */
    private static final class BoundedInputStream extends InputStream {

        private final InputStream delegate;
        private final long maxBytes;
        private long count;

        private BoundedInputStream(InputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b >= 0) {
                checkLimit(1);
            }
            return b;
        }

        @Override
        public int read(byte @NonNull [] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            if (n > 0) {
                checkLimit(n);
            }
            return n;
        }

        private void checkLimit(int n) throws IOException {
            if (count + n > maxBytes) {
                throw new UploadSizeLimitExceededException(
                    "文件大小超出限制: 实际读取超过 " + maxBytes + " 字节");
            }
            count += n;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    private static Map<String, MagicSignature> buildMagicSignatures() {
        Map<String, MagicSignature> map = new LinkedHashMap<>();

        MagicSignature jpeg = new MagicSignature(3, head ->
            (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF);
        map.put("jpg", jpeg);
        map.put("jpeg", jpeg);

        map.put("png", new MagicSignature(8, head ->
            head[0] == (byte) 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A));
        map.put("gif", new MagicSignature(6, head ->
            matchesPrefix(head, 0, "GIF87a") || matchesPrefix(head, 0, "GIF89a")));
        map.put("bmp", new MagicSignature(2, head -> matchesPrefix(head, 0, "BM")));
        map.put("webp", new MagicSignature(12, head ->
            matchesPrefix(head, 0, "RIFF") && matchesPrefix(head, 8, "WEBP")));

        map.put("pdf", new MagicSignature(5, head -> matchesPrefix(head, 0, "%PDF-")));

        MagicSignature zip = new MagicSignature(4, head -> matchesPrefix(head, 0, "PK")
            && ((head[2] & 0xFF) == 3 || (head[2] & 0xFF) == 5 || (head[2] & 0xFF) == 7)
            && ((head[3] & 0xFF) == 4 || (head[3] & 0xFF) == 6 || (head[3] & 0xFF) == 8));
        map.put("zip", zip);
        map.put("docx", zip);
        map.put("xlsx", zip);
        map.put("pptx", zip);

        // 旧版 OLE2 复合文档（doc/xls/ppt）
        map.put("doc", new MagicSignature(8, head ->
            (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF && (head[2] & 0xFF) == 0x11
                && (head[3] & 0xFF) == 0xE0 && (head[4] & 0xFF) == 0xA1 && (head[5] & 0xFF) == 0xB1
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0xE1));
        map.put("xls", map.get("doc"));
        map.put("ppt", map.get("doc"));

        map.put("rar", new MagicSignature(7, head ->
            matchesPrefix(head, 0, "Rar!\u001A\u0007\u0000")));
        map.put("7z", new MagicSignature(6, head ->
            matchesPrefix(head, 0, "7z¼¯'\u001C")));
        map.put("mp3", new MagicSignature(3, head ->
            (head[0] == 'I' && head[1] == 'D' && head[2] == '3')
                || ((head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0)));
        map.put("mp4", new MagicSignature(8, head -> matchesPrefix(head, 4, "ftyp")));

        return Collections.unmodifiableMap(map);
    }

    private static boolean matchesPrefix(byte[] head, int offset, String ascii) {
        byte[] prefix = ascii.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        if (offset + prefix.length > head.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (head[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
