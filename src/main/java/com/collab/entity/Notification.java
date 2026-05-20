package com.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息持久化
 */
@Data
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收人
     */
    private Long receiverId;

    /**
     * 发送人
     */
    private Long senderId;

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
     * 是否已读
     */
    private Integer isRead;

    private LocalDateTime createTime;
}