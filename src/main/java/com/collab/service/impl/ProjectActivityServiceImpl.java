package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.entity.ProjectActivity;
import com.collab.mapper.ProjectActivityMapper;
import com.collab.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private final ProjectActivityMapper projectActivityMapper;

    @Override
    public void addActivity(ProjectActivity activity) {

        projectActivityMapper.insert(activity);
    }

    @Override
    public List<ProjectActivity> getList(Long projectId) {

        return projectActivityMapper.selectList(

                new LambdaQueryWrapper<ProjectActivity>()

                        .eq(
                                ProjectActivity::getProjectId,
                                projectId
                        )

                        .orderByDesc(
                                ProjectActivity::getCreateTime
                        )
        );
    }
}