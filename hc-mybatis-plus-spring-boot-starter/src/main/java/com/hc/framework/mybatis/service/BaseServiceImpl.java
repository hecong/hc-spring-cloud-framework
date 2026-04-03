package com.hc.framework.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hc.framework.mybatis.model.PageParam;
import com.hc.framework.mybatis.model.PageResult;

/**
 * 基础 Service 实现类
 * <p>
 * 继承自 MyBatis-Plus 的 ServiceImpl，实现 BaseService 接口
 *
 * @param <M> Mapper 类型
 * @param <T> 实体类型
 * @author hc
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {

    @Override
    public PageResult<T> pageResult(PageParam pageParam) {
        IPage<T> page = page(pageParam.toPage());
        return PageResult.success(page);
    }

    @Override
    public PageResult<T> pageResult(PageParam pageParam, Wrapper<T> queryWrapper) {
        IPage<T> page = page(pageParam.toPage(), queryWrapper);
        return PageResult.success(page);
    }
}
