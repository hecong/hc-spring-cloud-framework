package com.hnhegui.framework.excel.service.impl;

import com.hnhegui.framework.excel.service.ExcelFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 * Excel文件本地存储实现
 *
 * <p>默认实现，仅将文件保存在本地临时目录，返回本地文件路径。</p>
 * <p>引用方可以实现 {@link ExcelFileStorage} 接口并注册为Spring Bean来覆盖此实现，
 * 实现云端存储（OSS、S3、MinIO等）。</p>
 *
 * <p>此实现由 {@code ExcelAutoConfiguration} 通过 @Bean 显式注册，
 * 确保在引用项目不能扫描到框架包时仍能正常工作。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class LocalExcelFileStorage implements ExcelFileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalExcelFileStorage.class);

    @Override
    public String upload(File file, String taskId) {
        // 本地存储直接返回文件路径
        String filePath = file.getAbsolutePath();
        if (log.isDebugEnabled()) {
            log.debug("[Excel文件存储]本地存储 - taskId: {}, filePath: {}", taskId, filePath);
        }
        return filePath;
    }

    @Override
    public String upload(InputStream inputStream, String fileName, String taskId) {
        // 不支持流上传到本地，返回空
        log.warn("[Excel文件存储]本地存储不支持流上传 - taskId: {}, fileName: {}", taskId, fileName);
        return null;
    }

    @Override
    public void delete(String fileUrl) {
        // 本地文件删除
        if (fileUrl != null && !fileUrl.startsWith("http")) {
            File file = new File(fileUrl);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (log.isDebugEnabled()) {
                    log.debug("[Excel文件存储]删除本地文件 - filePath: {}, deleted: {}", fileUrl, deleted);
                }
            }
        }
    }
}
