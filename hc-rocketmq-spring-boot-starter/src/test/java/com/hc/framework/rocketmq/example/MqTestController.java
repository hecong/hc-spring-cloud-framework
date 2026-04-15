package com.hc.framework.rocketmq.example;

import com.hc.framework.rocketmq.core.RocketMqSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * MQ 测试 Controller
 *
 * <p>提供各种消息类型的发送测试接口</p>
 *
 * @author hc-framework
 */
@Slf4j
@RestController
@RequestMapping("/mq/test")
@RequiredArgsConstructor
public class MqTestController {

    private final RocketMqSender rocketMqSender;

    /**
     * 发送普通消息
     *
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/normal")
    public String sendNormal(@RequestParam(defaultValue = "Hello RocketMQ") String content) {
        TestMessageDTO dto = buildDto(content);
        SendReceipt receipt = rocketMqSender.send("TEST_NORMAL_TOPIC", "TEST_TAG", dto);
        return "普通消息发送成功: " + receipt.getMessageId();
    }

    /**
     * 发送延迟消息
     *
     * @param content 消息内容
     * @param delayMs 延迟时间（毫秒）
     * @return 发送结果
     */
    @GetMapping("/delay")
    public String sendDelay(
            @RequestParam(defaultValue = "Hello Delay") String content,
            @RequestParam(defaultValue = "5000") long delayMs) {
        TestMessageDTO dto = buildDto(content);
        SendReceipt receipt = rocketMqSender.sendDelay("TEST_NORMAL_TOPIC", "TEST_TAG", dto, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        return "延迟消息发送成功，延迟 " + delayMs + "ms: " + receipt.getMessageId();
    }

    /**
     * 发送顺序消息
     *
     * @param content 消息内容
     * @param hashKey 哈希键
     * @return 发送结果
     */
    @GetMapping("/orderly")
    public String sendOrderly(
            @RequestParam(defaultValue = "Hello Orderly") String content,
            @RequestParam(defaultValue = "order-1") String hashKey) {
        TestMessageDTO dto = buildDto(content);
        SendReceipt receipt = rocketMqSender.sendOrderly("TEST_ORDER_TOPIC", "TEST_TAG", dto, hashKey);
        return "顺序消息发送成功，hashKey " + hashKey + ": " + receipt.getMessageId();
    }

    /**
     * 发送批量消息
     *
     * @param count 消息数量
     * @return 发送结果
     */
    @GetMapping("/batch")
    public String sendBatch(@RequestParam(defaultValue = "10") int count) {
        List<TestMessageDTO> list = IntStream.range(0, count)
                .mapToObj(i -> buildDto("Batch Message " + i))
                .toList();
        rocketMqSender.sendBatch("TEST_BATCH_TOPIC", "TEST_TAG", list);
        return "批量消息已发送，数量 " + count;
    }

    /**
     * 发送异步消息
     *
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/async")
    public String sendAsync(@RequestParam(defaultValue = "Hello Async") String content) {
        TestMessageDTO dto = buildDto(content);
        rocketMqSender.sendAsync("TEST_NORMAL_TOPIC", "TEST_TAG", dto);
        return "异步消息已发送";
    }

    /**
     * 发送单向消息
     *
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/oneway")
    public String sendOneway(@RequestParam(defaultValue = "Hello Oneway") String content) {
        TestMessageDTO dto = buildDto(content);
        rocketMqSender.sendOneway("TEST_NORMAL_TOPIC", "TEST_TAG", dto);
        return "单向消息已发送";
    }

    /**
     * 发送事务消息
     *
     * @param content 消息内容
     * @return 发送结果
     */
    @GetMapping("/transaction")
    public String sendTransaction(
            @RequestParam(defaultValue = "Hello Transaction") String content) {
        TestMessageDTO dto = buildDto(content);
        // 发送事务消息
        var result = rocketMqSender.sendTransaction("TEST_TRANS_TOPIC", "TEST_TAG", dto);

        try {
            // 执行本地事务（如数据库操作）
            // doLocalTransaction();

            // 提交事务
            result.getTransaction().commit();
            return "事务消息发送成功并提交: " + result.getMessageId();
        } catch (Exception e) {
            // 回滚事务
            try {
                result.getTransaction().rollback();
            } catch (Exception rollbackEx) {
                log.error("事务回滚失败", rollbackEx);
            }
            return "事务消息已回滚: " + e.getMessage();
        }
    }

    /**
     * 构建测试 DTO
     */
    private TestMessageDTO buildDto(String content) {
        TestMessageDTO dto = new TestMessageDTO();
        dto.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        dto.setContent(content);
        dto.setSendTime(LocalDateTime.now());
        return dto;
    }

}
