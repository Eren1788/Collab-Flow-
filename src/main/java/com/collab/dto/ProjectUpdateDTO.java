package com.collab.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectUpdateDTO {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;
}