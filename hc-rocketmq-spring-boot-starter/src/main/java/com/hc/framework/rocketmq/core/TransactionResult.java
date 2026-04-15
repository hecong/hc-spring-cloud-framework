package com.hc.framework.rocketmq.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;

/**
 * 事务消息发送结果
 *
 * <p>封装事务消息发送后的返回结果，包含：</p>
 * <ul>
 *     <li>sendReceipt: 发送回执（包含消息 ID）</li>
 *     <li>transaction: 事务对象（用于提交或回滚事务）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * TransactionResult result = rocketMqSender.sendTransaction("TOPIC", "TAG", data);
 * try {
 *     // 执行本地事务（如数据库操作）
 *     doLocalTransaction();
 *     result.getTransaction().commit();
 * } catch (Exception e) {
 *     result.getTransaction().rollback();
 * }
 * }</pre>
 *
 * @author hc-framework
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {

    /**
     * 发送回执（包含消息 ID）
     */
    private SendReceipt sendReceipt;

    /**
     * 事务对象（用于提交或回滚事务）
     */
    private Transaction transaction;

    /**
     * 获取消息 ID
     *
     * @return 消息 ID
     */
    public String getMessageId() {
        return sendReceipt != null && sendReceipt.getMessageId() != null
                ? sendReceipt.getMessageId().toString()
                : null;
    }

}
