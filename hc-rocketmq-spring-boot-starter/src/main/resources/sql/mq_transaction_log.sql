-- =============================================================================
-- hc-rocketmq-spring-boot-starter 事务消息日志表
--
-- 用途：仅用于事务回查时判断本地事务是否已提交。
-- 与本地消息表方案的本质区别：
--   事务消息的投递状态由 RocketMQ Broker 管理，
--   本地不需要存储消息体、发送状态、重试次数。
--   保存此记录的唯一目的：Broker 回查时通过 msg_id 判断本地事务是否已提交。
--
-- 清理策略：无需清理。事务终态（COMMIT/ROLLBACK）后 Broker 不再回查。
-- =============================================================================

CREATE TABLE IF NOT EXISTS `mq_transaction_log` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `msg_id`      VARCHAR(64)  NOT NULL COMMENT '消息唯一标识（用于回查匹配）',
    `topic`       VARCHAR(128) NOT NULL COMMENT '消息主题',
    `tag`         VARCHAR(128) NOT NULL DEFAULT '*' COMMENT '消息标签',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_id` (`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ事务消息日志表';
