package com.collab.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectVO {

    private Long id;

    private String name;

    private String description;

    private Integer status;

    private String creatorName;

    private Integer taskCount;

    private Integer completedTaskCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}