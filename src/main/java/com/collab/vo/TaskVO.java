package com.collab.vo;

import lombok.Data;

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
}