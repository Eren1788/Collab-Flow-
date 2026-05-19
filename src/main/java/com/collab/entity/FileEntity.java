package com.collab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file")
public class FileEntity {

    @TableId
    private Long id;

    private String fileName;

    private String fileUrl;

    private Long uploaderId;

    private Long projectId;

    private LocalDateTime createTime;
}