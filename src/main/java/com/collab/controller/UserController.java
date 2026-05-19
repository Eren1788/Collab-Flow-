package com.collab.controller;

import com.collab.common.result.Result;

import com.collab.common.utils.JwtUtils;
import com.collab.dto.LoginDTO;
import com.collab.entity.User;
import com.collab.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户相关接口")
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    /**
     * 用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "用户列表", description = "查询所有用户信息")
    public Result<List<User>>list(){
        log.info("查询所有用户");
        return Result.success(
                userService.list()
        );
    }

    /**
     *新增用户
     */
    @PostMapping("/register")
    @Operation(summary = "添加用户", description = "新增用户信息")
    public Result add(@RequestBody User user){
        log.info("新增用户:{}",user);
        userService.save(user);
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口，返回token")
    public Result<String> login(@RequestBody LoginDTO dto) {
        log.info("开始登录:{}", dto);
        User user = userService.lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .one();

        //判断这个user是否为空
        if (user == null){
            return Result.error("这个用户不存在，请重新输入");
        }
        //判断密码是否正确
        if (!user.getPassword().equals(dto.getPassword())){
            return Result.error("密码错误,请重新输入");
        }
        //都正确，直接生成token
        String token = JwtUtils.creatToken(
                user.getId(),
                user.getUsername()
        );
        return Result.success(token);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public Result<User> info(HttpServletRequest request){
        log.info("获取当前用户登录");
        Long userId = (Long) request.getAttribute("userId");

        User user = userService.getById(userId);

        return Result.success(user);
    }
}
