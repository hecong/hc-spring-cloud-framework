package com.hc.framework.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.support.OssTestSupport;
import com.hc.framework.oss.support.OssTestSupport.TrackingInputStream;
import com.hc.framework.oss.support.OssUploadValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 阿里云上传编排单测（mock SDK，验证 3.1 校验失败/成功路径均关流、3.2 头部未丢失、3.4 大小超限解包）
 */
class AliyunOssServiceImplUploadTest {

    private static final String ENDPOINT = "oss-cn-hangzhou.aliyuncs.com";
    private static final String BUCKET = "hc-test-bucket";

    private final OSS oss = mock(OSS.class);
    private final AtomicReference<byte[]> sdkReceived = new AtomicReference<>();

    private AliyunOssServiceImpl service(OssUploadValidator validator) {
        OssProperties.AliyunOssConfig config = new OssProperties.AliyunOssConfig();
        config.setEndpoint(ENDPOINT);
        config.setBucketName(BUCKET);
        return new AliyunOssServiceImpl(config, validator, oss);
    }

    private void stubSdkConsumesStream() {
        doAnswer(invocation -> {
            InputStream stream = invocation.getArgument(2);
            sdkReceived.set(OssTestSupport.consumeAll(stream)); // 模拟 SDK 完整读流
            return null;
        }).when(oss).putObject(eq(BUCKET), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    private void assertSdkNotCalled() {
        verifyNoInteractions(oss);
    }

    @Test
    @DisplayName("合法图片上传成功：内容从头完整消费、SDK 收到原文件头、流已关闭")
    void uploadSuccessKeepsHeadAndCloses() {
        stubSdkConsumesStream();
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        String url = service(new OssUploadValidator())
                .upload("photo.jpg", source, "image/jpeg", content.length);

        assertTrue(url.startsWith("https://" + BUCKET + "." + ENDPOINT + "/photo.jpg"), url);
        assertArrayEquals(content, sdkReceived.get(), "SDK 应收到从头部开始的完整内容");
        assertEquals(content.length, source.consumed());
        assertTrue(source.closed(), "成功后原始流必须已关闭");
        verify(oss, times(1)).putObject(eq(BUCKET), eq("photo.jpg"), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    @DisplayName("伪造内容（PNG 伪装 .jpg）：IllegalArgumentException、SDK 不收到请求、流已关闭")
    void fakeContentRejectedBeforeSdk() {
        stubSdkConsumesStream();
        byte[] png = OssTestSupport.png();
        TrackingInputStream source = new TrackingInputStream(png);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(new OssUploadValidator()).upload("photo.jpg", source, "image/jpeg", png.length));

        assertTrue(e.getMessage().contains("内容与扩展名不符"), e.getMessage());
        assertSdkNotCalled();
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("高风险扩展名：SDK 不收到请求、原始流未被读取即被关闭")
    void forbiddenExtensionRejectedBeforeAnyRead() {
        stubSdkConsumesStream();
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(new OssUploadValidator()).upload("shell.jsp", source, "image/jpeg", content.length));

        assertTrue(e.getMessage().contains("不允许的文件类型"), e.getMessage());
        assertSdkNotCalled();
        assertEquals(0, source.consumed(), "扩展名预检必须先于任何读流");
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("声明大小超限：IllegalArgumentException、SDK 不收到请求、流已关闭")
    void declaredOversizeRejectedBeforeSdk() {
        stubSdkConsumesStream();
        OssUploadValidator validator = new OssUploadValidator(true, null, 10);
        byte[] content = OssTestSupport.text("this content is longer than 10 bytes");
        TrackingInputStream source = new TrackingInputStream(content);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(validator).upload("note.txt", source, "text/plain", content.length));

        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
        assertSdkNotCalled();
        assertEquals(0, source.consumed());
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("未知大小(-1)读流过程超限：SDK 包装的标记异常被解包为 IllegalArgumentException")
    void streamingOversizeUnwrappedAsIllegalArgument() {
        OssUploadValidator validator = new OssUploadValidator(true, null, 8);
        stubSdkConsumesStream();
        byte[] content = OssTestSupport.zip(20);
        TrackingInputStream source = new TrackingInputStream(content);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(validator).upload("archive.zip", source, "application/zip", -1));

        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("SDK 网络/服务异常（非校验标记）：仍包装为运行时上传失败异常")
    void sdkFailureStillWrappedAsUploadError() {
        doAnswer(invocation -> {
            throw new RuntimeException("service unavailable");
        }).when(oss).putObject(eq(BUCKET), anyString(), any(InputStream.class), any(ObjectMetadata.class));

        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> service(new OssUploadValidator()).upload("photo.jpg", source, "image/jpeg", content.length));

        assertEquals("文件上传失败", e.getMessage());
        assertTrue(source.closed());
    }
}
