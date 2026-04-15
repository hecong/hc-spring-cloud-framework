package com.hc.framework.rocketmq.example;

import com.hc.framework.rocketmq.core.BaseMqMessage;
import com.hc.framework.rocketmq.core.BaseTransactionChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQTransactionListener;
import org.springframework.stereotype.Component;

/**
 * 事务消息监听器示例
 *
 * <p>注意：事务监听器需要配合自定义的 RocketMQClientTemplate 使用</p>
 *
 *
 * @author hc-framework
 */
@Slf4j
@Component
@RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQClientTemplate")
public class TransactionMessageChecker extends BaseTransactionChecker {



    @Override
    protected boolean doCheckTransaction(BaseMqMessage msg) {
        // 回查：查询本地事务状态
//        String state = stateMapper.getState(msg.getMsgId());
//        log.info("事务回查 msgId:{} state:{}", msg.getMsgId(), state);
        return true;
    }

}
