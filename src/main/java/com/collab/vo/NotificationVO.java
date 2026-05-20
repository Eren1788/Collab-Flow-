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
}