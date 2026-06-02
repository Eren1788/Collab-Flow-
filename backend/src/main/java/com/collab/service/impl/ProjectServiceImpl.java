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

import java.time.format.DateTimeFormatter;
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
    private final FileInfoMapper fileInfoMapper;
    private final TaskExecutorMapper taskExecutorMapper;  // 新增：用于多执行人查询

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

        // 获取当前登录用户
        Long currentUserId = LoginUserContext.getUserId();
        // 判断是否为超级管理员（roleId = 1）
        Long roleId = userRoleMapper.getRoleIdByUserId(currentUserId);
        boolean isAdmin = (roleId != null && roleId == 1L);

        // 如果不是超级管理员，则只查询该用户参与的项目
        if (!isAdmin) {
            LambdaQueryWrapper<ProjectMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(ProjectMember::getUserId, currentUserId);
            List<ProjectMember> members = projectMemberMapper.selectList(memberWrapper);

            if (members.isEmpty()) {
                Page<ProjectVO> empty = new Page<>(pageNum, pageSize);
                empty.setRecords(Collections.emptyList());
                empty.setTotal(0);
                return empty;
            }

            List<Long> projectIds = members.stream()
                    .map(ProjectMember::getProjectId)
                    .collect(Collectors.toList());
            wrapper.in(Project::getId, projectIds);
        }

        wrapper.orderByDesc(Project::getId);
        Page<Project> projectPage = projectMapper.selectPage(page, wrapper);
        Page<ProjectVO> result = new Page<>();
        BeanUtils.copyProperties(projectPage, result);
        List<ProjectVO> records = projectPage.getRecords()
                .stream()
                .map(this::buildProjectVO)
                .collect(Collectors.toList());
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

            if (member.getJoinTime() != null) {
                map.put("joinTime", member.getJoinTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                map.put("joinTime", "");
            }

            list.add(map);
        }
        return list;
    }

    @Override
    public void deleteMember(Long id) {
        projectMemberMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getProjectStatistics(Long projectId) {
        // 获取项目成员
        LambdaQueryWrapper<ProjectMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ProjectMember::getProjectId, projectId);
        List<ProjectMember> members = projectMemberMapper.selectList(memberWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ProjectMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            if (user == null) continue;

            Map<String, Object> memberMap = new HashMap<>();
            memberMap.put("userId", user.getId());
            memberMap.put("username", user.getUsername());
            memberMap.put("nickname", user.getNickname());
            memberMap.put("avatar", user.getAvatar());

            // 统计该用户作为执行人的任务（通过 task_executor 表）
            LambdaQueryWrapper<TaskExecutor> teWrapper = new LambdaQueryWrapper<>();
            teWrapper.eq(TaskExecutor::getUserId, user.getId());
            List<TaskExecutor> teList = taskExecutorMapper.selectList(teWrapper);
            if (teList.isEmpty()) {
                memberMap.put("totalTasks", 0L);
                memberMap.put("completedTasks", 0L);
                memberMap.put("progress", 0);
                memberMap.put("files", new ArrayList<>());
                result.add(memberMap);
                continue;
            }
            List<Long> taskIds = teList.stream().map(TaskExecutor::getTaskId).collect(Collectors.toList());

            // 查询这些任务中属于当前项目的
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.in(Task::getId, taskIds);
            taskWrapper.eq(Task::getProjectId, projectId);
            List<Task> tasks = taskMapper.selectList(taskWrapper);

            long total = tasks.size();
            long completed = tasks.stream().filter(t -> t.getStatus() == 2).count();
            memberMap.put("totalTasks", total);
            memberMap.put("completedTasks", completed);
            memberMap.put("progress", total == 0 ? 0 : (int)(completed * 100 / total));

            // 查询该成员上传的文件（原有逻辑不变）
            List<Map<String, Object>> files = new ArrayList<>();
            for (Task task : tasks) {
                LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<>();
                fileWrapper.eq(FileInfo::getTaskId, task.getId());
                fileWrapper.eq(FileInfo::getUploaderId, user.getId());
                List<FileInfo> taskFiles = fileInfoMapper.selectList(fileWrapper);
                for (FileInfo file : taskFiles) {
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("id", file.getId());
                    fileMap.put("fileName", file.getName());
                    fileMap.put("fileSize", file.getFileSize());
                    fileMap.put("uploadTime", file.getUploadTime());
                    fileMap.put("taskTitle", task.getTitle());
                    files.add(fileMap);
                }
            }
            memberMap.put("files", files);
            result.add(memberMap);
        }
        return result;
    }

    private ProjectVO buildProjectVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);
        vo.setCreateTime(project.getCreateTime());
        User creator = userMapper.selectById(project.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getNickname());
        }
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProjectId, project.getId());
        Long taskCount = taskMapper.selectCount(wrapper);
        vo.setTaskCount(taskCount.intValue());
        return vo;
    }
}