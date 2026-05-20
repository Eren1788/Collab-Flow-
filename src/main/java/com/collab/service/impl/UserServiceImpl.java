package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.LoginDTO;
import com.collab.dto.UserRegisterDTO;
import com.collab.dto.UserUpdateDTO;
import com.collab.entity.User;
import com.collab.mapper.UserMapper;
import com.collab.service.UserService;
import com.collab.common.utils.JwtUtils;
import com.collab.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(User::getUsername,loginDTO.getUsername());

        User user = userMapper.selectOne(wrapper);

        if(user == null){
            throw new BusinessException("用户不存在");
        }

        if(!user.getPassword().equals(loginDTO.getPassword())){
            throw new BusinessException("密码错误");
        }

        //创建token
        String token = JwtUtils.creatToken(user.getId(),user.getUsername());

        Map<String,Object> map = new HashMap<>();

        map.put("token",token);

        return map;
    }

    @Override
    public void register(UserRegisterDTO dto) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(User::getUsername,dto.getUsername());

        Long count = userMapper.selectCount(wrapper);

        if(count > 0){
            throw new BusinessException("用户名已存在");
        }

        User user = new User();

        BeanUtils.copyProperties(dto,user);

        user.setRole("user");

        user.setStatus(1);

        userMapper.insert(user);
    }

    @Override
    public UserVO getCurrentUserInfo() {

        //Long userId = 1L;
        Long userId = LoginUserContext.getUserId();

        User user = userMapper.selectById(userId);

        UserVO vo = new UserVO();

        BeanUtils.copyProperties(user,vo);

        return vo;
    }

    @Override
    public List<UserVO> listUser() {

        List<User> users = userMapper.selectList(null);

        return users.stream().map(user -> {

            UserVO vo = new UserVO();

            BeanUtils.copyProperties(user,vo);

            return vo;

        }).collect(Collectors.toList());
    }

    @Override
    public Page<UserVO> pageUser(Integer pageNum,
                                 Integer pageSize,
                                 String keyword) {

        Page<User> page = new Page<>(pageNum,pageSize);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(
                StringUtils.hasText(keyword),
                User::getUsername,
                keyword
        );

        Page<User> userPage = userMapper.selectPage(page,wrapper);

        Page<UserVO> result = new Page<>();

        BeanUtils.copyProperties(userPage,result);

        List<UserVO> records =
                userPage.getRecords()
                        .stream()
                        .map(user -> {

                            UserVO vo = new UserVO();

                            BeanUtils.copyProperties(user,vo);

                            return vo;

                        }).collect(Collectors.toList());

        result.setRecords(records);

        return result;
    }

    @Override
    public UserVO detail(Long id) {

        User user = userMapper.selectById(id);

        UserVO vo = new UserVO();

        BeanUtils.copyProperties(user,vo);

        return vo;
    }

    @Override
    public void updateUser(UserUpdateDTO dto) {

        User user = new User();

        BeanUtils.copyProperties(dto,user);

        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}