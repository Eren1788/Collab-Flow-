package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.constant.RedisConstant;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.LoginDTO;
import com.collab.dto.UserRegisterDTO;
import com.collab.dto.UserUpdateDTO;
import com.collab.entity.User;
import com.collab.entity.UserRole;
import com.collab.mapper.UserMapper;
import com.collab.mapper.UserRoleMapper;
import com.collab.service.UserService;
import com.collab.common.utils.JwtUtils;
import com.collab.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String,Object> redisTemplate;
    private final UserRoleMapper userRoleMapper;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {

        // 构建查询条件：根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,loginDTO.getUsername());

        // 执行数据库查询，获取用户信息
        User user = userMapper.selectOne(wrapper);

        if(user == null){
            throw new BusinessException("用户不存在");
        }

        if(!user.getPassword().equals(loginDTO.getPassword())){
            throw new BusinessException("密码错误");
        }

        if(user.getStatus() != 1){
            throw new BusinessException("该账号已被禁用，请联系管理员admin");
        }

        //生成JWT
        String token = JwtUtils.creatToken(user.getId(),user.getUsername());

        //生成redis key：使用 userId 作为 key，避免长 token 浪费内存
        String redisKey = RedisConstant.LOGIN_TOKEN + user.getId();

        //保存redis：存储用户ID，过期时间 7 天
        redisTemplate.opsForValue().set(
                redisKey,
                user.getId(),
                7,
                TimeUnit.DAYS
        );

        Map<String,Object> map = new HashMap<>();
        map.put("token",token);
        map.put("userInfo",user);

        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)//加事务防止注册时涉及的两张表出现脏数据
    public void register(UserRegisterDTO dto) {
        /**
         * 1. 校验用户名是否存在
         */
        User existUser = lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .one();

        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        /**
         * 2. 创建用户
         */
        User user = new User();

        // 用户名
        user.setUsername(dto.getUsername());

        // 密码
        user.setPassword(dto.getPassword());

        // 昵称
        user.setNickname(dto.getNickname());

        // 邮箱
        user.setEmail(dto.getEmail());

        // 手机号
        user.setPhone(dto.getPhone());

        // 默认状态：启用
        user.setStatus(1);

        /**
         * 保存用户
         */
        save(user);

        /**
         * 3. 绑定角色
         */
        UserRole userRole = new UserRole();

        // 用户ID
        userRole.setUserId(user.getId());

        // 角色ID
        userRole.setRoleId(dto.getRoleId());

        /**
         * 保存用户角色关系
         */
        userRoleMapper.insert(userRole);
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
        return userMapper.selectUserListWithRole();
    }

    @Override
    public Page<UserVO> pageUser(Integer pageNum,
                                 Integer pageSize,
                                 String keyword) {

        Page<UserVO> page = new Page<>(pageNum, pageSize);

        return userMapper.selectUserPage(page, keyword);
    }

    @Override
    public UserVO detail(Long id) {

        User user = userMapper.selectById(id);

        UserVO vo = new UserVO();

        BeanUtils.copyProperties(user,vo);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User dbUser = userMapper.selectById(dto.getId());

        if (dbUser == null) {
            throw new BusinessException("用户不存在");
        }

        /**
         * 普通信息修改
         */
        if (dto.getNickname() != null) {
            dbUser.setNickname(dto.getNickname());
        }

        if (dto.getAvatar() != null) {
            dbUser.setAvatar(dto.getAvatar());
        }

        if (dto.getEmail() != null) {
            dbUser.setEmail(dto.getEmail());
        }

        if (dto.getPhone() != null) {
            dbUser.setPhone(dto.getPhone());
        }

        /**
         * 状态修改
         */
        if (dto.getStatus() != null) {

            Long currentUserId = LoginUserContext.getUserId();

            boolean isAdmin = userRoleMapper.existsAdminRole(currentUserId);

            if (!isAdmin) {
                throw new BusinessException("只有管理员才能修改用户状态");
            }

            dbUser.setStatus(dto.getStatus());
        }

        /**
         * 更新 user 表
         */
        userMapper.updateById(dbUser);

        /**
         * 修改角色（职位）
         */
        if (dto.getRoleId() != null) {

            LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();

            wrapper.eq(UserRole::getUserId, dto.getId());

            UserRole userRole = userRoleMapper.selectOne(wrapper);

            if (userRole != null) {

                userRole.setRoleId(dto.getRoleId());

                userRoleMapper.updateById(userRole);

            } else {

                UserRole newUserRole = new UserRole();

                newUserRole.setUserId(dto.getId());

                newUserRole.setRoleId(dto.getRoleId());

                userRoleMapper.insert(newUserRole);
            }
        }
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public void logout(HttpServletRequest request) {
        // 获取当前登录用户ID
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            return;
        }
        
        // 使用 userId 作为 Redis key，删除登录状态
        String redisKey = RedisConstant.LOGIN_TOKEN + userId;
        redisTemplate.delete(redisKey);
        log.info("用户 {} 已退出登录", userId);
    }
}