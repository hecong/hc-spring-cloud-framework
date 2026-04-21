package com.hc.framework.oss.service;

import java.io.InputStream;

/**
 * OSS服务接口
 */
public interface OssService {

    /**
     * 上传文件
     *
     * @param fileName    文件名
     * @param inputStream 文件流
     * @return 文件访问URL
     */
    String upload(String fileName, InputStream inputStream);

    /**
     * 上传文件
     *
     * @param fileName    文件名
     * @param inputStream 文件流
     * @param contentType 文件类型
     * @return 文件访问URL
     */
    String upload(String fileName, InputStream inputStream, String contentType);

    /**
     * 上传文件（指定内容长度）
     *
     * @param fileName      文件名
     * @param inputStream   文件流
     * @param contentType   文件类型
     * @param contentLength 文件内容长度（字节数），-1 表示未知
     * @return 文件访问URL
     */
    String upload(String fileName, InputStream inputStream, String contentType, long contentLength);

    /**
     * 删除文件
     *
     * @param fileName 文件名
     * @return 是否成功
     */
    boolean delete(String fileName);

    /**
     * 获取文件访问URL
     *
     * @param fileName 文件名
     * @return 文件URL
     */
    String getUrl(String fileName);

    /**
     * 获取文件访问URL（带过期时间）
     *
     * @param fileName   文件名
     * @param expireTime 过期时间（秒）
     * @return 文件URL
     */
    String getUrl(String fileName, Integer expireTime);

    /**
     * 检查文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    boolean exists(String fileName);
}
