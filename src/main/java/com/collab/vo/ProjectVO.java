package com.collab.vo;

import lombok.Data;

@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    private Integer status;

    private String creatorName;

    private Integer taskCount;
}