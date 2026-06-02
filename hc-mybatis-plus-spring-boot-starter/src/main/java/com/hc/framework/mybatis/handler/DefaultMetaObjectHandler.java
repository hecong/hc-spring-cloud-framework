package com.hc.framework.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.hc.framework.common.spi.UserIdProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 默认自动填充处理器
 *
 * <p>自动填充 createTime、updateTime、creator、updater、deleted 字段。</p>
 * <p>通过 {@link UserIdProvider} SPI 获取当前用户ID，配合 hc-satoken 时可零配置自动获取。</p>
 *
 * @author hc
 */
@Slf4j
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    private final UserIdProvider userIdProvider;

    public DefaultMetaObjectHandler(UserIdProvider userIdProvider) {
        this.userIdProvider = userIdProvider;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        LocalDateTime now = LocalDateTime.now();
        String userId = userIdProvider.getCurrentUserId();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "creator", String.class, userId);
        this.strictInsertFill(metaObject, "updater", String.class, userId);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        String userId = userIdProvider.getCurrentUserId();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updater", String.class, userId);
    }
}
