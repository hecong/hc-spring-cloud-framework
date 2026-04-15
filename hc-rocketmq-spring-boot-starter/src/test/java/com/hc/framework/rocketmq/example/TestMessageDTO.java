package com.hc.framework.rocketmq.example;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试消息 DTO
 *
 * @author hc-framework
 */
@Data
public class TestMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

}
