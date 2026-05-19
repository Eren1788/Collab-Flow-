package com.collab.dto;

import lombok.Data;

@Data
public class UserAddDTO {

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String role;
}