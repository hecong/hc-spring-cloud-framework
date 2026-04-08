package com.hc.framework.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hc.framework.mybatis.model.PageParam;
import com.hc.framework.mybatis.model.PageResult;

import java.util.List;

/**
 * 基础 Service 接口
 * <p>
 * 继承自 MyBatis-Plus 的 IService，提供通用的 CRUD 操作和统一分页
 *
 * @param <T> 实体类型
 * @author hc
 */
public interface BaseService<T> extends IService<T> {

    /**
     * 分页查询（返回统一分页结果）
     *
     * @param pageParam 分页参数
     * @return PageResult 统一分页响应
     */
    PageResult<T> pageResult(PageParam pageParam);

    /**
     * 条件分页查询（返回统一分页结果）
     *
     * @param pageParam 分页参数
     * @param queryWrapper 查询条件
     * @return PageResult 统一分页响应
     */
    PageResult<T> pageResult(PageParam pageParam, com.baomidou.mybatisplus.core.conditions.Wrapper<T> queryWrapper);

    /**
     * 批量插入（原生SQL，性能更高）
     *
     * @param list 实体列表
     * @return 插入行数
     */
    int insertBatch(List<T> list);

    /**
     * 批量插入或更新（原生SQL）
     *
     * @param list 实体列表
     * @return 影响行数
     */
    int insertOrUpdateBatch(List<T> list);
}
