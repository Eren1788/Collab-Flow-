package com.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long projectId;

    private String name;

    private String url;

    private Long fileSize;

    private String fileType;

    private Long uploaderId;

    private LocalDateTime uploadTime;
}