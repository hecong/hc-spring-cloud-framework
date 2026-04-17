package com.hc.framework.mybatis.model;

import com.hc.framework.common.constant.SystemConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页参数
 * <p>
 * 统一的分页请求参数，与 MyBatis-Plus 的 IPage 兼容
 *
 * @author hc
 */
@Data
public class PageParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = SystemConstants.DEFAULT_PAGE_NUM;

    /**
     * 每页条数
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 1000, message = "每页条数不能超过1000")
    private Integer pageSize = SystemConstants.DEFAULT_PAGE_SIZE;

    /**
     * 转换为 MyBatis-Plus 的 Page 对象
     *
     * @param <T> 数据类型
     * @return Page 对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
    }

    /**
     * 创建分页参数
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return PageParam
     */
    public static PageParam of(Integer pageNum, Integer pageSize) {
        PageParam param = new PageParam();
        param.setPageNum(pageNum);
        param.setPageSize(pageSize);
        return param;
    }
}
