package com.collab.service.impl;

import com.collab.mapper.RoleMapper;
import com.collab.service.RoleService;
import com.collab.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public List<RoleVO> getRoleList() {
        return roleMapper.getRoleList();
    }
}