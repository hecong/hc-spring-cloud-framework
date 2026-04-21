package com.hc.framework.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.hc.framework.mybatis.handler.DefaultMetaObjectHandler;
import com.hc.framework.mybatis.properties.MyBatisPlusProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * MyBatis-Plus 自动配置类
 *
 * @author hc
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(com.baomidou.mybatisplus.core.mapper.BaseMapper.class)
@EnableConfigurationProperties(MyBatisPlusProperties.class)
@ConditionalOnProperty(prefix = "hc.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MyBatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        if (properties.getPage().isEnabled()) {
            PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
            paginationInterceptor.setDbType(DbType.MYSQL);
            paginationInterceptor.setMaxLimit(properties.getPage().getMaxLimit());
            paginationInterceptor.setOverflow(properties.getPage().getOverflow());
            interceptor.addInnerInterceptor(paginationInterceptor);
            log.info("MyBatis-Plus 分页插件已启用");
        }

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }

    /**
     * 自动填充处理器
     * <p>
     * 默认使用 DefaultMetaObjectHandler，可通过实现 MetaObjectHandler 接口自定义
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler metaObjectHandler() {
        return new DefaultMetaObjectHandler();
    }
}
