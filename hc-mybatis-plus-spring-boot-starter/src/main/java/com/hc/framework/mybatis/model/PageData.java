package com.hc.framework.mybatis.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PageData<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer totalPage;
    private Boolean hasNext;

    // 全参构造
    public PageData(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPage = (int) Math.ceil((double) total / pageSize);
        this.hasNext = (long) pageNum * pageSize < total;
    }

    // 【核心工具方法】从 MyBatis-Plus IPage 快速构建 PageData
    public static <T> PageData<T> of(IPage<T> page) {
        return new PageData<>(
            page.getRecords(),
            page.getTotal(),
            (int) page.getCurrent(),
            (int) page.getSize()
        );
    }
}