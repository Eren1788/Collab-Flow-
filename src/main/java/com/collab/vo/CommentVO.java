package com.collab.vo;

import lombok.Data;

@Data
public class CommentVO {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 添加任务ID
     */
    private Long taskId;

    /**
     * 发送人用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论用户昵称
     */
    private String nickname;

    /**
     * 评论用户头像
     */
    private String avatar;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 评论时间
     */
    private String createTime;

    /**
     * 评论类型：TASK_COMMENT / PROJECT_COMMENT
     */
    private String type;

}