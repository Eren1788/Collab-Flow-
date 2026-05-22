package com.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.entity.User;
import com.collab.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 用户分页（联表查询角色）
     */
    Page<UserVO> selectUserPage(Page<UserVO> page, @Param("keyword") String keyword);

    /**
     * 查询所有用户及角色名称（list接口使用）
     */
    List<UserVO> selectUserListWithRole();
}