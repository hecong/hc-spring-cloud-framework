package com.hc.framework.oss.service.impl;

import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.support.OssTestSupport;
import com.hc.framework.oss.support.OssTestSupport.TrackingInputStream;
import com.hc.framework.oss.support.OssUploadValidator;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 腾讯 COS 上传编排单测：成功路径头部完整、校验失败 SDK 零交互、流式超限解包
 */
class TencentCosServiceImplUploadTest {

    private static final String REGION = "ap-guangzhou";
    private static final String BUCKET = "hc-test-1250000000";

    private final COSClient cosClient = mock(COSClient.class);
    private final AtomicReference<byte[]> sdkReceived = new AtomicReference<>();

    private TencentCosServiceImpl service(OssUploadValidator validator) {
        OssProperties.TencentCosConfig config = new OssProperties.TencentCosConfig();
        config.setRegion(REGION);
        config.setBucketName(BUCKET);
        config.setSecretId("secret-id");
        config.setSecretKey("secret-key");
        return new TencentCosServiceImpl(config, validator, cosClient);
    }

    private void stubSdkConsumesStream() {
        doAnswer(invocation -> {
            PutObjectRequest request = invocation.getArgument(0);
            sdkReceived.set(OssTestSupport.consumeAll(request.getInputStream())); // 模拟 SDK 完整读流
            return null;
        }).when(cosClient).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("合法图片上传成功：SDK 收到从头部开始的完整内容、流已关闭")
    void uploadSuccessKeepsHeadAndCloses() {
        stubSdkConsumesStream();
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        String url = service(new OssUploadValidator())
                .upload("photo.jpg", source, "image/jpeg", content.length);

        assertTrue(url.startsWith("https://" + BUCKET + ".cos." + REGION + ".myqcloud.com/photo.jpg"), url);
        assertArrayEquals(content, sdkReceived.get(), "SDK 应收到从头部开始的完整内容");
        assertEquals(content.length, source.consumed());
        assertTrue(source.closed(), "成功后原始流必须已关闭");
        verify(cosClient, times(1)).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("伪造内容：IllegalArgumentException、SDK 零交互、流已关闭")
    void fakeContentRejectedBeforeSdk() {
        stubSdkConsumesStream();
        byte[] png = OssTestSupport.png();
        TrackingInputStream source = new TrackingInputStream(png);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(new OssUploadValidator()).upload("photo.jpg", source, "image/jpeg", png.length));

        assertTrue(e.getMessage().contains("内容与扩展名不符"), e.getMessage());
        verifyNoInteractions(cosClient);
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("未知大小(-1)流式超限：读流阶段标记异常被解包为 IllegalArgumentException")
    void streamingOversizeUnwrapped() {
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
    @DisplayName("声明大小超限被拦截：不读取流、SDK 零交互、流已关闭")
    void declaredOversizeRejectedBeforeRead() {
        stubSdkConsumesStream();
        OssUploadValidator validator = new OssUploadValidator(true, null, 8);
        byte[] content = OssTestSupport.text("oversize declared content");
        TrackingInputStream source = new TrackingInputStream(content);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(validator).upload("note.txt", source, "text/plain", content.length));

        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
        assertEquals(0, source.consumed());
        assertTrue(source.closed());
    }
}
