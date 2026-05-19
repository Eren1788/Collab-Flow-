package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.dto.ProjectDTO;
import com.collab.dto.ProjectMemberDTO;
import com.collab.entity.Project;
import com.collab.entity.ProjectMember;
import com.collab.entity.Task;
import com.collab.entity.User;
import com.collab.mapper.ProjectMapper;
import com.collab.mapper.ProjectMemberMapper;
import com.collab.mapper.TaskMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.ProjectService;
import com.collab.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
        implements ProjectService {

    private final ProjectMapper projectMapper;

    private final ProjectMemberMapper projectMemberMapper;

    private final UserMapper userMapper;

    private final TaskMapper taskMapper;

    @Override
    public void addProject(ProjectDTO dto) {

        Project project = new Project();

        BeanUtils.copyProperties(dto,project);

        project.setCreatorId(1L);

        projectMapper.insert(project);

        // 自动添加创建人为项目管理员

        ProjectMember member = new ProjectMember();

        member.setProjectId(project.getId());

        member.setUserId(1L);

        member.setRole("admin");

        projectMemberMapper.insert(member);
    }

    @Override
    public List<ProjectVO> listProject() {

        List<Project> projects =
                projectMapper.selectList(null);

        return projects.stream().map(this::buildProjectVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProjectVO> pageProject(Integer pageNum,
                                       Integer pageSize,
                                       String keyword,
                                       Integer status) {

        Page<Project> page =
                new Page<>(pageNum,pageSize);

        LambdaQueryWrapper<Project> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.like(
                StringUtils.hasText(keyword),
                Project::getName,
                keyword
        );

        wrapper.eq(
                status != null,
                Project::getStatus,
                status
        );

        wrapper.orderByDesc(Project::getId);

        Page<Project> projectPage =
                projectMapper.selectPage(page,wrapper);

        Page<ProjectVO> result = new Page<>();

        BeanUtils.copyProperties(projectPage,result);

        List<ProjectVO> records =
                projectPage.getRecords()
                        .stream()
                        .map(this::buildProjectVO)
                        .collect(Collectors.toList());

        result.setRecords(records);

        return result;
    }

    @Override
    public ProjectVO detail(Long id) {

        Project project =
                projectMapper.selectById(id);

        return buildProjectVO(project);
    }

    @Override
    public void updateProject(ProjectDTO dto) {

        Project project = new Project();

        BeanUtils.copyProperties(dto,project);

        projectMapper.updateById(project);
    }

    @Override
    public void deleteProject(Long id) {

        // 删除项目

        projectMapper.deleteById(id);

        // 删除项目成员

        LambdaQueryWrapper<ProjectMember> memberWrapper =
                new LambdaQueryWrapper<>();

        memberWrapper.eq(
                ProjectMember::getProjectId,
                id
        );

        projectMemberMapper.delete(memberWrapper);

        // 删除项目任务

        LambdaQueryWrapper<Task> taskWrapper =
                new LambdaQueryWrapper<>();

        taskWrapper.eq(Task::getProjectId,id);

        taskMapper.delete(taskWrapper);
    }

    @Override
    public void addMember(ProjectMemberDTO dto) {

        LambdaQueryWrapper<ProjectMember> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(ProjectMember::getProjectId,
                dto.getProjectId());

        wrapper.eq(ProjectMember::getUserId,
                dto.getUserId());

        Long count =
                projectMemberMapper.selectCount(wrapper);

        if(count > 0){

            throw new RuntimeException("用户已存在项目中");
        }

        ProjectMember member = new ProjectMember();

        BeanUtils.copyProperties(dto,member);

        member.setRole("member");

        projectMemberMapper.insert(member);
    }

    @Override
    public List<Object> memberList(Long projectId) {

        LambdaQueryWrapper<ProjectMember> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(ProjectMember::getProjectId,
                projectId);

        List<ProjectMember> members =
                projectMemberMapper.selectList(wrapper);

        List<Object> list = new ArrayList<>();

        for (ProjectMember member : members) {

            User user =
                    userMapper.selectById(
                            member.getUserId()
                    );

            Map<String,Object> map =
                    new HashMap<>();

            map.put("id",member.getId());

            map.put("userId",user.getId());

            map.put("nickname",user.getNickname());

            map.put("avatar",user.getAvatar());

            map.put("role",member.getRole());

            list.add(map);
        }

        return list;
    }

    /**
     * 封装 ProjectVO
     */
    private ProjectVO buildProjectVO(Project project){

        ProjectVO vo = new ProjectVO();

        BeanUtils.copyProperties(project,vo);

        User creator =
                userMapper.selectById(
                        project.getCreatorId()
                );

        if(creator != null){

            vo.setCreatorName(
                    creator.getNickname()
            );
        }

        LambdaQueryWrapper<Task> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(Task::getProjectId,
                project.getId());

        Long taskCount =
                taskMapper.selectCount(wrapper);

        vo.setTaskCount(taskCount.intValue());

        return vo;
    }
}