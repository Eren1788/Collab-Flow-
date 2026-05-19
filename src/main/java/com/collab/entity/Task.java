package com.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 执行人ID
     */
    private Long executorId;

    /**
     * 0未开始 1进行中 2完成
     */
    private Integer status;

    /**
     * 0普通 1紧急
     */
    private Integer priority;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}