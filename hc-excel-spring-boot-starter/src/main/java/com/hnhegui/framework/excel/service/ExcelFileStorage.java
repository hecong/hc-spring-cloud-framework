package com.hnhegui.framework.excel.service;

import java.io.File;
import java.io.InputStream;

/**
 * Excel文件存储接口
 *
 * <p>用于将生成的Excel文件上传到云端存储（OSS、S3、MinIO等）。</p>
 * <p>引用方可以实现此接口并注册为Spring Bean来覆盖默认的本地存储实现。</p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>导出文件需要长期保存，不能仅存在服务器临时目录</li>
 *   <li>多实例部署时，文件需要在多个实例间共享</li>
 *   <li>需要通过URL直接访问导出的Excel文件</li>
 * </ul>
 *
 * <h3>自定义实现示例：</h3>
 * <pre>{@code
 * @Component
 * public class OssExcelFileStorage implements ExcelFileStorage {
 *     @Autowired
 *     private OSS ossClient;
 *
 *     @Override
 *     public String upload(File file, String taskId) {
 *         String objectName = "excel/export/" + taskId + ".xlsx";
 *         ossClient.putObject("my-bucket", objectName, file);
 *         return "https://my-bucket.oss-cn-beijing.aliyuncs.com/" + objectName;
 *     }
 *
 *     @Override
 *     public String upload(InputStream inputStream, String fileName, String taskId) {
 *         String objectName = "excel/export/" + taskId + ".xlsx";
 *         ossClient.putObject("my-bucket", objectName, inputStream);
 *         return "https://my-bucket.oss-cn-beijing.aliyuncs.com/" + objectName;
 *     }
 *
 *     @Override
 *     public void delete(String fileUrl) {
 *         String objectName = fileUrl.replace("https://my-bucket.oss-cn-beijing.aliyuncs.com/", "");
 *         ossClient.deleteObject("my-bucket", objectName);
 *     }
 * }
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public interface ExcelFileStorage {

    /**
     * 上传文件
     *
     * @param file   本地文件
     * @param taskId 任务ID（用于生成存储路径）
     * @return 文件访问URL
     */
    String upload(File file, String taskId);

    /**
     * 上传输入流
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param taskId      任务ID
     * @return 文件访问URL
     */
    String upload(InputStream inputStream, String fileName, String taskId);

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    void delete(String fileUrl);
}
