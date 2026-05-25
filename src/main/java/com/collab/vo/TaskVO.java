package com.collab.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    private Integer status;

    private Integer priority;

    private String projectName;

    private String creatorName;

    private List<String> executorNames;   // 执行人昵称列表

    private List<Long> executorIds;       // 执行人ID列表

    private LocalDateTime deadline;  // 对应数据库 end_time

    private Long creatorId;    // 任务创建者ID，用于回复时确定接收人

//    private Long executorId;

    private LocalDateTime createTime;
}