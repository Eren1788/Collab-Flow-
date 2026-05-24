package com.collab.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    private Integer status;

    private Integer priority;

    private String projectName;

    private String creatorName;

    private String executorName;

    private LocalDateTime deadline;  // 对应数据库 end_time

    private Long creatorId;    // 新增：任务创建者ID，用于回复时确定接收人

    private Long executorId;  // 新增
}