package com.collab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("role")
public class Role {

    @TableId
    private Long id;

    private String roleName;

    private String roleCode;
}