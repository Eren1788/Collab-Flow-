package com.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户实体")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String role;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}