package com.collab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permission")
public class Permission {

    @TableId
    private Long id;

    private String permissionName;

    private String permissionCode;
}