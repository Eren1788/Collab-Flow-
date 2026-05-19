package com.collab.dto;

import lombok.Data;

@Data
public class TaskDTO {

    private Long id;

    private Long projectId;

    private String title;

    private String content;

    private Long executorId;

    private Integer priority;

    private Integer status;
}