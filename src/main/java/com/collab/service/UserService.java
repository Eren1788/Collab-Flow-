package com.collab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.dto.LoginDTO;
import com.collab.dto.UserRegisterDTO;
import com.collab.dto.UserUpdateDTO;
import com.collab.entity.User;
import com.collab.vo.UserVO;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {

    Map<String, Object> login(LoginDTO loginDTO);

    void register(UserRegisterDTO dto);

    UserVO getCurrentUserInfo();

    List<UserVO> listUser();

    Page<UserVO> pageUser(Integer pageNum,
                          Integer pageSize,
                          String keyword);

    UserVO detail(Long id);

    void updateUser(UserUpdateDTO dto);

    void deleteUser(Long id);

}