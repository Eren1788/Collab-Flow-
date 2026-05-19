package com.collab.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    private String creatorName;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}