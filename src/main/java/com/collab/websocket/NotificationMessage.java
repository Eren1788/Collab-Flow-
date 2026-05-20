package com.collab.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知消息结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    /**
     * 通知类型
     */
    private String type;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 时间
     */
    private Long timestamp;
}