package com.hc.framework.oss.service.impl;

import com.hc.framework.oss.config.OssProperties;
import com.hc.framework.oss.support.OssTestSupport;
import com.hc.framework.oss.support.OssTestSupport.TrackingInputStream;
import com.hc.framework.oss.support.OssUploadValidator;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
 * MinIO 上传编排单测：成功路径头部完整、校验失败 SDK 零交互、未知大小(-1)流式超限解包
 */
class MinioOssServiceImplUploadTest {

    private static final String ENDPOINT = "http://127.0.0.1:9000";
    private static final String BUCKET = "hc-minio-bucket";

    private final MinioClient minio = mock(MinioClient.class);
    private final AtomicReference<byte[]> sdkReceived = new AtomicReference<>();

    private MinioOssServiceImpl service(OssUploadValidator validator) {
        OssProperties.MinioConfig config = new OssProperties.MinioConfig();
        config.setEndpoint(ENDPOINT);
        config.setBucketName(BUCKET);
        config.setAccessKey("minioadmin");
        config.setSecretKey("minioadmin");
        return new MinioOssServiceImpl(config, validator, minio);
    }

    private void stubSdkConsumesStream() throws Exception {
        doAnswer(invocation -> {
            PutObjectArgs args = invocation.getArgument(0);
            sdkReceived.set(OssTestSupport.consumeAll(args.stream())); // 模拟 SDK 完整读流
            return null;
        }).when(minio).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("合法图片上传成功：SDK 收到从头部开始的完整内容、流已关闭")
    void uploadSuccessKeepsHeadAndCloses() throws Exception {
        stubSdkConsumesStream();
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        String url = service(new OssUploadValidator())
                .upload("photo.jpg", source, "image/jpeg", content.length);

        assertEquals(ENDPOINT + "/" + BUCKET + "/photo.jpg", url);
        assertArrayEquals(content, sdkReceived.get(), "SDK 应收到从头部开始的完整内容");
        assertEquals(content.length, source.consumed());
        assertTrue(source.closed(), "成功后原始流必须已关闭");
        verify(minio, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("伪造内容：IllegalArgumentException、SDK 零交互、流已关闭")
    void fakeContentRejectedBeforeSdk() throws Exception {
        stubSdkConsumesStream();
        byte[] png = OssTestSupport.png();
        TrackingInputStream source = new TrackingInputStream(png);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(new OssUploadValidator()).upload("photo.jpg", source, "image/jpeg", png.length));

        assertTrue(e.getMessage().contains("内容与扩展名不符"), e.getMessage());
        verifyNoInteractions(minio);
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("未知大小(-1)流式超限：读流阶段触发标记异常并被解包为 IllegalArgumentException")
    void streamingOversizeUnwrapped() throws Exception {
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
    @DisplayName("声明大小超限：扩展名预检后拦截，流被关闭")
    void declaredOversizeRejected() throws Exception {
        stubSdkConsumesStream();
        OssUploadValidator validator = new OssUploadValidator(true, null, 8);
        byte[] content = OssTestSupport.text("oversize declared content");
        TrackingInputStream source = new TrackingInputStream(content);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service(validator).upload("note.txt", source, "text/plain", content.length));

        assertTrue(e.getMessage().contains("大小超出限制"), e.getMessage());
        assertTrue(source.closed());
    }

    @Test
    @DisplayName("禁用校验时扩展名拦截失效（仅走原始行为），流仍由框架关闭")
    void validationDisabledPassesThroughButStillCloses() throws Exception {
        stubSdkConsumesStream();
        OssUploadValidator disabled = new OssUploadValidator(false, null, OssUploadValidator.DEFAULT_MAX_FILE_SIZE);
        byte[] content = OssTestSupport.jpeg();
        TrackingInputStream source = new TrackingInputStream(content);

        String url = service(disabled).upload("shell.jsp", source, "application/octet-stream", -1);

        assertEquals(ENDPOINT + "/" + BUCKET + "/shell.jsp", url);
        assertTrue(source.closed(), "即使校验禁用，框架仍应关闭调用方传入的流");
    }
}
