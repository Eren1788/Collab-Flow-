package com.collab.service;

import com.collab.entity.ProjectActivity;

import java.util.List;

public interface ProjectActivityService {

    /**
     * 新增动态
     */
    void addActivity(ProjectActivity activity);

    /**
     * 项目动态列表
     */
    List<ProjectActivity> getList(Long projectId);
}