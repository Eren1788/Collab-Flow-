package com.collab.service;

import com.collab.vo.RoleVO;

import java.util.List;

public interface RoleService {

    /**
     * 获取角色列表
     */
    List<RoleVO> getRoleList();
}