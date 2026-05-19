package com.collab.dto;

import lombok.Data;

@Data
public class CommentDTO {

    private Long taskId;

    private String content;

    private Long parentId;
}