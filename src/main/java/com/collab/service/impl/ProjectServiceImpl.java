package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.ProjectDTO;
import com.collab.dto.ProjectMemberDTO;
import com.collab.entity.*;
import com.collab.mapper.*;
import com.collab.service.ProjectActivityService;
import com.collab.service.ProjectService;
import com.collab.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ProjectActivityService projectActivityService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public void addProject(ProjectDTO dto) {
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        project.setCreatorId(LoginUserContext.getUserId());
        projectMapper.insert(project);

        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(project.getId());
        activity.setUserId(LoginUserContext.getUserId());
        activity.setType("PROJECT_CREATE");
        activity.setContent("创建了项目：" + project.getName());
        projectActivityService.addActivity(activity);

        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getId());
        member.setUserId(LoginUserContext.getUserId());
        member.setRole("admin");
        projectMemberMapper.insert(member);
    }

    @Override
    public List<ProjectVO> listProject() {
        List<Project> projects = projectMapper.selectList(null);
        return projects.stream().map(this::buildProjectVO).collect(Collectors.toList());
    }

    @Override
    public Page<ProjectVO> pageProject(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<Project> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Project::getName, keyword);
        wrapper.eq(status != null, Project::getStatus, status);
        wrapper.orderByDesc(Project::getId);
        Page<Project> projectPage = projectMapper.selectPage(page, wrapper);
        Page<ProjectVO> result = new Page<>();
        BeanUtils.copyProperties(projectPage, result);
        List<ProjectVO> records = projectPage.getRecords().stream().map(this::buildProjectVO).collect(Collectors.toList());
        result.setRecords(records);
        return result;
    }

    @Override
    public ProjectVO detail(Long id) {
        Project project = projectMapper.selectById(id);
        return buildProjectVO(project);
    }

    @Override
    public void updateProject(ProjectDTO dto) {
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        projectMapper.updateById(project);
    }

    @Override
    public void deleteProject(Long id) {
        projectMapper.deleteById(id);
        LambdaQueryWrapper<ProjectMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ProjectMember::getProjectId, id);
        projectMemberMapper.delete(memberWrapper);
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(Task::getProjectId, id);
        taskMapper.delete(taskWrapper);
    }

    @Override
    public void addMember(ProjectMemberDTO dto) {
        LambdaQueryWrapper<ProjectMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectMember::getProjectId, dto.getProjectId());
        wrapper.eq(ProjectMember::getUserId, dto.getUserId());
        Long count = projectMemberMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("用户已存在项目中");
        }
        ProjectMember member = new ProjectMember();
        BeanUtils.copyProperties(dto, member);
        member.setRole("member");
        projectMemberMapper.insert(member);

        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(dto.getProjectId());
        activity.setUserId(dto.getUserId());
        activity.setType("MEMBER_ADD");
        activity.setContent("新增项目成员");
        projectActivityService.addActivity(activity);
    }

    @Override
    public List<Object> memberList(Long projectId) {
        LambdaQueryWrapper<ProjectMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectMember::getProjectId, projectId);
        List<ProjectMember> members = projectMemberMapper.selectList(wrapper);

        List<Object> list = new ArrayList<>();
        for (ProjectMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            if (user == null) {
                log.warn("项目成员 userId={} 不存在于 user 表中，已跳过", member.getUserId());
                continue;
            }
            // 查询用户的系统职位（roleName）
            Long roleId = userRoleMapper.getRoleIdByUserId(user.getId());
            String systemRoleName = "";
            if (roleId != null) {
                systemRoleName = roleMapper.getRoleNameById(roleId);
            }
            if (systemRoleName == null || systemRoleName.isEmpty()) {
                systemRoleName = "普通成员";
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", member.getId());
            map.put("userId", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("avatar", user.getAvatar());
            map.put("role", systemRoleName);
            list.add(map);
        }
        return list;
    }

    @Override
    public void deleteMember(Long id) {
        projectMemberMapper.deleteById(id);
    }

    private ProjectVO buildProjectVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);
        // 设置创建时间
        vo.setCreateTime(project.getCreateTime());
        // 设置创建人昵称
        User creator = userMapper.selectById(project.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getNickname());
        }
        // 设置任务数量
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProjectId, project.getId());
        Long taskCount = taskMapper.selectCount(wrapper);
        vo.setTaskCount(taskCount.intValue());
        return vo;
    }
}