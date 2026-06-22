package com.hc.framework.rocketmq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * MQ 事务消息日志实体（轻量级，仅 4 字段用于回查判断）
 *
 * <p>与 hnhegui {@code message_record} 表（15 字段）的区别：
 * 事务消息的投递状态由 RocketMQ Broker 管理，本地不需要存储消息体、发送状态、重试次数。
 * 保存此记录的唯一目的：Broker 回查时通过 {@code msgId} 判断本地事务是否已提交。</p>
 *
 * <p>业务方只需执行建表 DDL，无需写任何 Java 代码。</p>
 *
 * @author hc-framework
 * @since 1.0.0
 */
@Data
@TableName("mq_transaction_log")
public class MqTransactionLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 消息唯一标识（用于回查匹配，唯一索引） */
    private String msgId;

    /** 消息主题 */
    private String topic;

    /** 消息标签 */
    private String tag;

    /** 创建时间 */
    private Date createTime;
}
