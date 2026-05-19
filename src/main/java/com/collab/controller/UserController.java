package com.collab.controller;

import com.collab.common.result.Result;

import com.collab.common.utils.JwtUtils;
import com.collab.dto.LoginDTO;
import com.collab.entity.User;
import com.collab.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
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
     * 获取当前用户登录
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request){
        log.info("获取当前用户登录");
        Long userId = (Long) request.getAttribute("userId");

        User user = userService.getById(userId);

        return Result.success(user);
    }
}
