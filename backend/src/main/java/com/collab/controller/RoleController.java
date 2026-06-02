package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.service.RoleService;
import com.collab.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@CrossOrigin
public class RoleController {

    private final RoleService roleService;

    /**
     * 获取角色职位列表
     */
    @GetMapping("/list")
    public Result<List<RoleVO>> getRoleList() {

        List<RoleVO> list = roleService.getRoleList();

        return Result.success(list);
    }
}