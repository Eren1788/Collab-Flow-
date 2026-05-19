package com.collab.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;

    private String title;

    private String content;

    private String projectName;

    private String creatorName;

    private String executorName;

    private Integer status;

    private Integer priority;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}