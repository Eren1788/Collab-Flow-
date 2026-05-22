package com.collab.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private Long id;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    /**
     * 用户状态
     * 1 启用
     * 0 禁用
     */
    private Integer status;
}