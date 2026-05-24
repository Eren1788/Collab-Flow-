package com.collab.vo;

import lombok.Data;

@Data
public class NotificationVO {

    private Long id;

    private String type;

    private String content;

    private Long businessId;

    private Integer isRead;

    private String createTime;

    // 新增字段
    private String senderName;   // 发送人昵称/用户名
    private String avatar;          //发送人头像URL
    private String projectName;  // 项目名称
    private String taskTitle;    // 任务标题
}