package com.collab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {

    private Long id;

    private Long projectId;

    private String title;

    private String content;

    private List<Long> executorIds; //多人执行ID列表

    private Integer status;

    private Integer priority;

    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
}