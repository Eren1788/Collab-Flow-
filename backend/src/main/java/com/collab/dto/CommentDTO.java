package com.collab.dto;

import lombok.Data;

@Data
public class CommentDTO {

    /**
     * 任务ID（与 projectId 二选一）
     */
    private Long taskId;

    /**
     * 项目ID（与 taskId 二选一）
     */
    private Long projectId;

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