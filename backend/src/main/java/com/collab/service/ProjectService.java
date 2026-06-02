package com.collab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.dto.ProjectDTO;
import com.collab.dto.ProjectMemberDTO;
import com.collab.entity.Project;
import com.collab.vo.ProjectVO;


import java.util.List;
import java.util.Map;

public interface ProjectService extends IService<Project> {

    void addProject(ProjectDTO dto);

    List<ProjectVO> listProject();

    Page<ProjectVO> pageProject(Integer pageNum, Integer pageSize, String keyword, Integer status);

    ProjectVO detail(Long id);

    void updateProject(ProjectDTO dto);

    void deleteProject(Long id);

    void addMember(ProjectMemberDTO dto);

    List<Object> memberList(Long projectId);

    void deleteMember(Long id);

    List<Map<String, Object>> getProjectStatistics(Long projectId);

}