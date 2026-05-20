package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.dto.LoginDTO;
import com.collab.dto.UserRegisterDTO;
import com.collab.dto.UserUpdateDTO;
import com.collab.service.UserService;
import com.collab.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户模块",description = "用户相关的接口")
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录",description = "用户登录接口")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO){
        log.info("用户登录:{}",loginDTO);
        return Result.success(userService.login(loginDTO));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册",description = "用户注册接口")
    public Result<Void> register( @Validated @RequestBody UserRegisterDTO dto){
        log.info("用户注册:{}",dto);
        userService.register(dto);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息",description = "获取当前用户信息接口")
    public Result<UserVO> getUserInfo(){
        log.info("获取当前用户信息:");
        return Result.success(userService.getCurrentUserInfo());
    }

    /**
     * 用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "用户列表",description = "用户列表接口")
    public Result<Object> list(){
        log.info("用户列表");
        return Result.success(userService.listUser());
    }

    /**
     * 用户分页
     */
    @GetMapping("/page")
    @Operation(summary = "用户分页",description = "用户分页接口")
    public Result<Object> page(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String keyword
    ){
        log.info("用户分页:{},{},{}",pageNum,pageSize,keyword);
        return Result.success(
                userService.pageUser(pageNum,pageSize,keyword)
        );
    }

    /**
     * 用户详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "用户详情",description = "用户详情接口")
    public Result<UserVO> detail(@PathVariable Long id){
        log.info("用户详情:{}",id);
        return Result.success(userService.detail(id));
    }

    /**
     * 修改用户
     */
    @PutMapping("/update")
    @Operation(summary = "修改用户",description = "修改用户接口")
    public Result<Void> update(@RequestBody UserUpdateDTO dto){
        log.info("修改用户:{}",dto);
        userService.updateUser(dto);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除用户",description = "删除用户接口")
    public Result<Void> delete(@PathVariable Long id){
        log.info("删除用户:{}",id);
        userService.deleteUser(id);
        return Result.success();
    }

}