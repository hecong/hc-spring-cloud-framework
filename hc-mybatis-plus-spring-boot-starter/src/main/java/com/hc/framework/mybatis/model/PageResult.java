package com.hc.framework.mybatis.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分页结果
 * <p>
 * 统一的分页响应结果，与 web 框架的 Result 结构保持一致
 *
 * @param <T> 数据类型
 * @author hc
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 分页数据
     */
    private PageData<T> data;

    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 请求路径
     */
    private String path;

    public PageResult() {
        this.timestamp = LocalDateTime.now();
    }

    public PageResult(Integer code, String message, PageData<T> data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 成功响应 ====================

    /**
     * 创建成功响应
     *
     * @param list       数据列表
     * @param total      总条数
     * @param pageNum    当前页码
     * @param pageSize   每页条数
     * @param <T>        数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> success(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        return new PageResult<>(HttpStatus.OK.value(), "操作成功", 
                new PageData<>(list, total, pageNum, pageSize));
    }

    /**
     * 从 MyBatis-Plus 的 IPage 创建成功响应
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> success(IPage<T> page) {
        return new PageResult<>(HttpStatus.OK.value(), "操作成功",
                new PageData<>(page.getRecords(), page.getTotal(), 
                        (int) page.getCurrent(), (int) page.getSize()));
    }

    /**
     * 创建成功响应（自定义消息）
     *
     * @param message 自定义消息
     * @param page    MyBatis-Plus 分页结果
     * @param <T>     数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> success(String message, IPage<T> page) {
        return new PageResult<>(HttpStatus.OK.value(), message,
                new PageData<>(page.getRecords(), page.getTotal(),
                        (int) page.getCurrent(), (int) page.getSize()));
    }

    // ==================== 失败响应 ====================

    /**
     * 创建失败响应（默认错误码）
     *
     * @param <T> 数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> error() {
        return new PageResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "操作失败", null);
    }

    /**
     * 创建失败响应（自定义消息）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> error(String message) {
        return new PageResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    /**
     * 创建失败响应（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return PageResult
     */
    public static <T> PageResult<T> error(Integer code, String message) {
        return new PageResult<>(code, message, null);
    }

    // ==================== 状态判断 ====================

    /**
     * 判断是否为成功响应
     *
     * @return true=成功
     */
    public boolean isSuccess() {
        return HttpStatus.OK.value() == this.code;
    }

    // ==================== 内部类：分页数据 ====================

    /**
     * 分页数据
     *
     * @param <T> 数据类型
     */
    @Data
    public static class PageData<T> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 数据列表
         */
        private List<T> list;

        /**
         * 总条数
         */
        private Long total;

        /**
         * 当前页码
         */
        private Integer pageNum;

        /**
         * 每页条数
         */
        private Integer pageSize;

        /**
         * 总页数
         */
        private Integer totalPage;

        /**
         * 是否有下一页
         */
        private Boolean hasNext;

        public PageData() {
        }

        public PageData(List<T> list, Long total, Integer pageNum, Integer pageSize) {
            this.list = list;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.totalPage = (int) Math.ceil((double) total / pageSize);
            this.hasNext = (long) pageNum * pageSize < total;
        }
    }
}
