package com.collab.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 职位名称
     */
    private String roleName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 状态
     * 0 禁用
     * 1 正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 权限列表
     */
    private List<String> permissions;
}