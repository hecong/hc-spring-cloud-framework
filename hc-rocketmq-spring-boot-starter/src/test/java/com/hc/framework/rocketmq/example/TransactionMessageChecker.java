package com.hc.framework.rocketmq.example;

import com.hc.framework.rocketmq.core.transaction.BaseTransactionChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQTransactionListener;
import org.springframework.stereotype.Component;

/**
 * 事务消息监听器示例（泛型版）
 *
 * <p>直接操作业务 DTO，无需手动调用 {@code TestMessageDTO.from(msg)}。</p>
 *
 * @author hc-framework
 */
@Slf4j
@Component
@RocketMQTransactionListener
public class TransactionMessageChecker extends BaseTransactionChecker<TestMessageDTO> {

    @Override
    protected boolean doCheckTransaction(TestMessageDTO dto) {
        // 回查：查询本地事务状态
//        String state = stateMapper.getStateByOrderNo(dto.getOrderNo());
//        log.info("事务回查 orderNo:{} state:{}", dto.getOrderNo(), state);
        return true;
    }

}
