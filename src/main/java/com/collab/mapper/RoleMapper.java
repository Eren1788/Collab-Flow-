package com.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.entity.Role;
import com.collab.vo.RoleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 获取角色列表
     */
    @Select("""
            SELECT
                id,
                role_name AS roleName,
                role_code AS roleCode,
                description
            FROM role
            ORDER BY id
            """)
    List<RoleVO> getRoleList();

    String getRoleNameById(Long roleId);
}