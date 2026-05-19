package com.collab.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("LoginDTO")
public class LoginDTO {

    private String username;

    private String password;
}
