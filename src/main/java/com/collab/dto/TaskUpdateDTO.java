package com.collab.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskUpdateDTO {

    private Long id;

    private String title;

    private String content;

    private Long executorId;

    private Integer status;

    private Integer priority;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}