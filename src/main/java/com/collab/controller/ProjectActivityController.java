package com.collab.controller;

import com.collab.common.exception.BusinessException;
import com.collab.common.result.Result;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.ProjectActivity;
import com.collab.mapper.UserRoleMapper;
import com.collab.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project/activity")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService projectActivityService;
    private final UserRoleMapper userRoleMapper;

    /**
     * 动态列表
     */
    @GetMapping("/list/{projectId}")
    public Result<List<ProjectActivity>> list(@PathVariable Long projectId) {
        return Result.success(projectActivityService.getList(projectId));
    }

    /**
     * 删除动态（仅管理员和项目经理）
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = LoginUserContext.getUserId();
        Long roleId = userRoleMapper.getRoleIdByUserId(userId);
        boolean isAdmin = roleId != null && roleId == 1L;
        boolean isPM = roleId != null && roleId == 2L;
        if (!isAdmin && !isPM) {
            throw new BusinessException("只有管理员和项目经理可以删除动态");
        }
        projectActivityService.removeById(id);
        return Result.success();
    }
}