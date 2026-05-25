package com.collab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.common.annotation.OperationLogAnnotation;
import com.collab.common.result.Result;
import com.collab.dto.ProjectDTO;
import com.collab.dto.ProjectMemberDTO;
import com.collab.service.ProjectService;
import com.collab.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 新增项目
     */
    @OperationLogAnnotation("新增项目")//使用aop
    @PostMapping("/add")
    public Result<Void> add(@RequestBody ProjectDTO dto){

        projectService.addProject(dto);

        return Result.success();
    }

    /**
     * 项目列表
     */
    @GetMapping("/list")
    public Result<Object> list(){

        return Result.success(projectService.listProject());
    }

    /**
     * 项目分页
     */
    @GetMapping("/page")
    public Result<Page<ProjectVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(projectService.pageProject(pageNum, pageSize, keyword, status));
    }

    /**
     * 项目详情
     */
    @GetMapping("/detail/{id}")
    public Result<ProjectVO> detail(@PathVariable Long id){

        return Result.success(projectService.detail(id));
    }

    /**
     * 修改项目
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody ProjectDTO dto){

        projectService.updateProject(dto);

        return Result.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id){

        projectService.deleteProject(id);

        return Result.success();
    }

    /**
     * 添加项目成员
     */
    @PostMapping("/member/add")
    public Result<Void> addMember(
            @RequestBody ProjectMemberDTO dto
    ){

        projectService.addMember(dto);

        return Result.success();
    }

    /**
     * 项目成员列表
     */
    @GetMapping("/member/list/{projectId}")
    public Result<Object> memberList(
            @PathVariable Long projectId
    ){

        return Result.success(
                projectService.memberList(projectId)
        );
    }

    /**
     * 删除项目成员
     */
    @DeleteMapping("/member/delete/{id}")
    public Result<Void> deleteMember(
            @PathVariable Long id
    ){

        projectService.deleteMember(id);

        return Result.success();
    }

    /**
     * 项目成员完成情况统计
     */
    @GetMapping("/statistics/{projectId}")
    public Result<Object> statistics(@PathVariable Long projectId) {
        return Result.success(projectService.getProjectStatistics(projectId));
    }

}