package com.collab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId
    private Long id;

    private Long taskId;

    private Long userId;

    private String content;

    private LocalDateTime createTime;
}