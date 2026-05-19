package com.collab.dto;

import lombok.Data;

@Data
public class CommentDTO {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID
     * 0 = 一级评论
     */
    private Long parentId;

}