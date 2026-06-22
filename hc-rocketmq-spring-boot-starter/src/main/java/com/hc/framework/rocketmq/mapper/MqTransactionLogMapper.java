package com.hc.framework.rocketmq.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hc.framework.rocketmq.entity.MqTransactionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ 事务日志 Mapper
 *
 * <p>框架内置，业务方无需编写。由 {@code MapperScan} 自动扫描。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Mapper
public interface MqTransactionLogMapper extends BaseMapper<MqTransactionLog> {

    /**
     * 根据 msgId 判断记录是否存在（用于事务回查）。
     *
     * <p>走唯一索引 {@code uk_msg_id}，性能最优。</p>
     *
     * @param msgId 消息 ID
     * @return true 表示记录存在（即本地事务已提交）
     */
    default boolean existsByMsgId(String msgId) {
        return selectCount(new LambdaQueryWrapper<MqTransactionLog>()
                .eq(MqTransactionLog::getMsgId, msgId)) > 0;
    }
}
