package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.entity.ProjectActivity;
import com.collab.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project/activity")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService
            projectActivityService;

    /**
     * 动态列表
     */
    @GetMapping("/list/{projectId}")
    public Result<List<ProjectActivity>> list(

            @PathVariable Long projectId
    ){

        return Result.success(

                projectActivityService.getList(projectId)
        );
    }
}