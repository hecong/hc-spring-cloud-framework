package com.hc.framework.oss.support;

import com.hc.framework.oss.support.OssTestSupport.TrackingInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OssUploadValidator 校验链单测：
 * 1.1 扩展名白名单 / 1.2 魔数校验与文件头回推 / 1.3 未知大小限制读取流
 */
class OssUploadValidatorTest {

    // ---------- 1.1 扩展名白名单 ----------

    @Test
    @DisplayName("默认白名单覆盖常见图片/文档/文本/压缩/音视频，且不包含高风险后缀")
    void defaultWhitelistCoversCommonTypes() {
        OssUploadValidator validator = new OssUploadValidator();
        assertEquals("jpg", validator.validate("photo.jpg", 10));
        assertEquals("pdf", validator.validate("a.PDF", 10), "大小写应归一化");
        // 无后缀默认拒绝（README 无扩展名）
        assertThrows(IllegalArgumentException.class, () -> validator.validate("README", 10));
        for (String forbidden : List.of("jsp", "exe", "js", "php", "sh", "html")) {
            assertFalse(validator.getAllowedExtensions().contains(forbidden), forbidden + " 不应在白名单");
        }
    }

    @Test
    @DisplayName("拒绝高风险/无后缀扩展名与空文件名，异常消息可区分")
    void rejectsForbiddenExtensions() {
        OssUploadValidator validator = new OssUploadValidator();
        for (String fileName : List.of("shell.jsp", "install.exe", "README", "dir/run.php")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> validator.validate(fileName, 10), "应拒绝: " + fileName);
            assertTrue(e.getMessage().contains("不允许的文件类型"), fileName + " -> " + e.getMessage());
        }
        assertThrows(IllegalArgumentException.class, () -> validator.validate("   ", 10));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null, 10));
    }

    @Test
    @DisplayName("自定义扩展名白名单整体覆盖内置表（默认表不使用叠加）")
    void customExtensionsOverrideDefault() {
        OssUploadValidator validator = new OssUploadValidator(true, List.of(".log", "TXT"), 1024);
        assertEquals("log", validator.validate("app.log", 10));
        assertEquals("txt", validator.validate("a.txt", 10), "前导点号与大写自动归一化");
        assertFalse(validator.getAllowedExtensions().contains("jpg"), "自定义表不叠加内置表");
        assertThrows(IllegalArgumentException.class, () -> validator.validate("a.jpg", 10));
    }

    @Test
    @DisplayName("声明大小超过上限直接拒绝（不读取流）；等于上限放行")
    void declaredSizeExceededRejected() {
        OssUploadValidator validator = new OssUploadValidator(true, null, 100);
        validator.validate("note.txt", 100);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("note.txt", 101));
        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
    }

    @Test
    @DisplayName("空白名单或非正上限回落内置默认值")
    void fallsBackToDefaults() {
        OssUploadValidator validator = new OssUploadValidator(true, List.of(), -5);
        assertEquals(OssUploadValidator.DEFAULT_ALLOWED_EXTENSIONS, validator.getAllowedExtensions());
        assertEquals(OssUploadValidator.DEFAULT_MAX_FILE_SIZE, validator.getMaxFileSize());
    }

    @Test
    @DisplayName("禁用校验：所有检查跳过，原流原样透传")
    void disabledSkipsAllChecks() {
        OssUploadValidator validator = new OssUploadValidator(false, List.of("jpg"), 10);
        assertNull(validator.validate("shell.jsp", 9999));
        InputStream source = new TrackingInputStream(OssTestSupport.png());
        assertSame(source, validator.prepare("shell.jsp", source, -1), "禁用时不得包装/读取原流");
    }

    // ---------- 1.2 魔数校验与文件头回推 ----------

    @Test
    @DisplayName("合法魔数放行；比对后文件头回推，SDK 仍从头完整消费")
    void validMagicPassesAndHeadIsPushedBack() throws IOException {
        OssUploadValidator validator = new OssUploadValidator();
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        InputStream prepared = validator.prepare("photo.jpg", source, content.length);
        byte[] reread = prepared.readAllBytes();
        prepared.close();

        assertArrayEquals(content, reread, "回推后读取的完整内容应与原文件一致（头部未丢失/未错位）");
        assertEquals(content.length, source.consumed(), "底层流应被恰好完整消费一次");
        assertTrue(source.closed(), "包装流关闭应级联关闭底层流");
    }

    @Test
    @DisplayName("伪造后缀：PNG 内容伪装成 .jpg 被拒（魔术校验拦截）")
    void fakeExtensionRejectedByMagic() {
        OssUploadValidator validator = new OssUploadValidator();
        byte[] png = OssTestSupport.png();
        TrackingInputStream source = new TrackingInputStream(png);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> validator.prepare("photo.jpg", source, png.length));
        assertTrue(e.getMessage().contains("内容与扩展名不符"), e.getMessage());
    }

    @Test
    @DisplayName("文本类扩展名（txt）无魔数要求：任意文本负载放行")
    void textTypeSkipsMagicCheck() throws IOException {
        OssUploadValidator validator = new OssUploadValidator();
        byte[] content = OssTestSupport.text("hello world, not a magic file");
        TrackingInputStream source = new TrackingInputStream(content);
        InputStream prepared = validator.prepare("note.txt", source, content.length);
        assertArrayEquals(content, prepared.readAllBytes());
        prepared.close();
    }

    @Test
    @DisplayName("文件头长度不足签名长度即判伪造（空/极短内容不因不足而放行）")
    void tooShortHeadRejected() {
        OssUploadValidator validator = new OssUploadValidator();
        byte[] content = new byte[]{'P', 'K'};
        assertThrows(IllegalArgumentException.class,
                () -> validator.prepare("archive.zip", new TrackingInputStream(content), content.length));
    }

    // ---------- 1.3 未知大小限制读取流 ----------

    @Test
    @DisplayName("未知大小(-1) 读取超过上限：读流过程抛大小超限标记异常")
    void unknownLengthOverLimitThrowsMarker() {
        OssUploadValidator validator = new OssUploadValidator(true, null, 8);
        byte[] content = OssTestSupport.zip(13); // 13 > 8
        InputStream prepared = validator.prepare("archive.zip", new TrackingInputStream(content), -1);

        IOException e = assertThrows(IOException.class, prepared::readAllBytes);
        assertInstanceOf(OssUploadValidator.UploadSizeLimitExceededException.class, e, "异常类型应为标记异常: " + e);
        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
    }

    @Test
    @DisplayName("未知大小恰好等于上限：完整放行")
    void unknownLengthEqualsLimitPasses() throws IOException {
        OssUploadValidator validator = new OssUploadValidator(true, null, 8);
        byte[] content = OssTestSupport.zip(8);
        InputStream prepared = validator.prepare("archive.zip", new TrackingInputStream(content), -1);
        assertArrayEquals(content, prepared.readAllBytes());
    }
}
