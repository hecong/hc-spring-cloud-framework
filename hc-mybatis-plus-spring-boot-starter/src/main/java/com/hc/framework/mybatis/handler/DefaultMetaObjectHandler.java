package com.hc.framework.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 默认自动填充处理器
 * <p>
 * 提供基础的字段自动填充功能，支持通过 SPI 机制扩展获取当前用户ID
 *
 * @author hc
 */
@Slf4j
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    /**
     * 当前用户ID提供者
     */
    private static Supplier<String> currentUserIdSupplier = () -> null;

    /**
     * 注册当前用户ID提供者
     *
     * @param supplier 用户ID提供者
     */
    public static void registerCurrentUserIdSupplier(Supplier<String> supplier) {
        currentUserIdSupplier = supplier != null ? supplier : () -> null;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        LocalDateTime now = LocalDateTime.now();
        String userId = currentUserIdSupplier.get();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "creator", String.class, userId);
        this.strictInsertFill(metaObject, "updater", String.class, userId);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        String userId = currentUserIdSupplier.get();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updater", String.class, userId);
    }
}
