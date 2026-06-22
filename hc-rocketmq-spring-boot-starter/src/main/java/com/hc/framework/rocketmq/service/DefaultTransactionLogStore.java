package com.hc.framework.rocketmq.service;

import com.hc.framework.rocketmq.core.BaseMqMessage;
import com.hc.framework.rocketmq.core.transaction.TransactionLogStore;
import com.hc.framework.rocketmq.entity.MqTransactionLog;
import com.hc.framework.rocketmq.mapper.MqTransactionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * TransactionLogStore 默认实现（基于 MyBatis-Plus）
 *
 * <p>框架内置，业务方零代码。启用条件：</p>
 * <ol>
 *     <li>类路径存在 MyBatis-Plus（{@code SqlSessionFactory}）</li>
 *     <li>执行建表 DDL：{@code CREATE TABLE mq_transaction_log (...)}</li>
 * </ol>
 *
 * <p>如果业务方需要自定义（例如额外字段），实现 {@link TransactionLogStore} 并注册为 Bean，
 * 框架的 {@code @ConditionalOnMissingBean} 会自动跳过此默认实现。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultTransactionLogStore implements TransactionLogStore {

    private final MqTransactionLogMapper mapper;

    @Override
    public void save(BaseMqMessage message) {
        MqTransactionLog log = new MqTransactionLog();
        log.setMsgId(message.getMsgId());
        log.setTopic(message.getTopic());
        log.setTag(message.getTag());
        log.setCreateTime(new Date());
        mapper.insert(log);
    }

    @Override
    public boolean existsByMsgId(String msgId) {
        return mapper.existsByMsgId(msgId);
    }
}
