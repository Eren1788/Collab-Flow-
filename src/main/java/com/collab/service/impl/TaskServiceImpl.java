package com.collab.service.impl;

import com.collab.entity.*;
import com.collab.mapper.*;
import com.collab.service.NotificationService;
import com.collab.service.ProjectActivityService;
import com.collab.websocket.NotificationMessage;
import com.collab.websocket.NotificationWebSocketHandler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.TaskAssignDTO;
import com.collab.dto.TaskDTO;
import com.collab.dto.TaskStatusDTO;
import com.collab.service.TaskService;
import com.collab.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;
    private final UserRoleMapper userRoleMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TaskExecutorMapper taskExecutorMapper;  // 新增
    private final TaskReadStatusMapper taskReadStatusMapper;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTask(TaskDTO dto) {
        // 1. 创建任务（注意：Task 实体中已无 executorId 字段）
        Task task = new Task();
        BeanUtils.copyProperties(dto, task);  // 只复制除 executorIds 外的公共属性
        task.setCreatorId(LoginUserContext.getUserId());
        taskMapper.insert(task);

        // 2. 处理多执行人关联
        if (dto.getExecutorIds() != null && !dto.getExecutorIds().isEmpty()) {
            for (Long userId : dto.getExecutorIds()) {
                TaskExecutor te = new TaskExecutor();
                te.setTaskId(task.getId());
                te.setUserId(userId);
                taskExecutorMapper.insert(te);
            }
        }

        // 3. 项目动态
        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(task.getProjectId());
        activity.setUserId(LoginUserContext.getUserId());
        activity.setType("TASK_CREATE");
        activity.setContent("创建了任务：" + task.getTitle());
        projectActivityService.addActivity(activity);

        // 4. 通知所有执行人
        if (dto.getExecutorIds() != null && !dto.getExecutorIds().isEmpty()) {
            String content = "你有一个新的任务：" + task.getTitle();
            for (Long executorId : dto.getExecutorIds()) {
                notificationService.saveNotification(executorId, LoginUserContext.getUserId(), "TASK_CREATE", content, task.getId());
                NotificationMessage message = new NotificationMessage("TASK_CREATE", content, task.getId(), System.currentTimeMillis());
                NotificationWebSocketHandler.sendMessage(executorId, message);
            }
        }
    }

    @Override
    public List<TaskVO> listTask() {
        List<Task> tasks = taskMapper.selectList(null);
        return tasks.stream().map(this::buildTaskVO).collect(Collectors.toList());
    }

    @Override
    public Page<TaskVO> pageTask(
            Integer pageNum,
            Integer pageSize,
            Long projectId,
            Integer status,
            Long executorId,
            Integer priority,
            String keyword
    ) {
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(Task::getProjectId, projectId);
        }
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        if (priority != null) {
            wrapper.eq(Task::getPriority, priority);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Task::getTitle, keyword);
        }

        // 按执行人过滤
        if (executorId != null) {
            LambdaQueryWrapper<TaskExecutor> teWrapper = new LambdaQueryWrapper<>();
            teWrapper.eq(TaskExecutor::getUserId, executorId);
            List<TaskExecutor> teList = taskExecutorMapper.selectList(teWrapper);
            if (teList.isEmpty()) {
                Page<TaskVO> empty = new Page<>(pageNum, pageSize);
                empty.setRecords(Collections.emptyList());
                empty.setTotal(0);
                return empty;
            }
            List<Long> taskIds = teList.stream().map(TaskExecutor::getTaskId).collect(Collectors.toList());
            wrapper.in(Task::getId, taskIds);
        }

        wrapper.orderByDesc(Task::getId);
        Page<Task> taskPage = taskMapper.selectPage(page, wrapper);
        Page<TaskVO> result = new Page<>();
        BeanUtils.copyProperties(taskPage, result);
        List<TaskVO> records = taskPage.getRecords().stream().map(this::buildTaskVO).collect(Collectors.toList());

        // 计算每个任务的未读评论数（当前登录用户）
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId != null && !records.isEmpty()) {
            List<Long> taskIds = records.stream().map(TaskVO::getId).collect(Collectors.toList());
            // 获取每个任务的最新已读时间
            LambdaQueryWrapper<TaskReadStatus> readWrapper = new LambdaQueryWrapper<>();
            readWrapper.eq(TaskReadStatus::getUserId, currentUserId);
            readWrapper.in(TaskReadStatus::getTaskId, taskIds);
            List<TaskReadStatus> readStatusList = taskReadStatusMapper.selectList(readWrapper);
            Map<Long, LocalDateTime> lastReadMap = readStatusList.stream()
                    .collect(Collectors.toMap(TaskReadStatus::getTaskId, TaskReadStatus::getLastReadTime));

            // 定义一个 MySQL 可接受的最小日期（1970-01-01 00:00:00）
            LocalDateTime DEFAULT_MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
            for (TaskVO taskVO : records) {
                LocalDateTime lastRead = lastReadMap.getOrDefault(taskVO.getId(), DEFAULT_MIN_DATE);
                LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
                commentWrapper.eq(Comment::getTaskId, taskVO.getId());
                commentWrapper.gt(Comment::getCreateTime, lastRead);
                Long unreadCount = commentMapper.selectCount(commentWrapper);
                taskVO.setUnreadCount(unreadCount.intValue());
            }
        } else {
            // 未登录或没有任务，设置未读数为 0
            records.forEach(taskVO -> taskVO.setUnreadCount(0));
        }

        result.setRecords(records);
        return result;
    }

    @Override
    public TaskVO detail(Long id) {
        Task task = taskMapper.selectById(id);
        return buildTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TaskDTO dto) {
        // 1. 更新任务基本信息
        Task task = new Task();
        BeanUtils.copyProperties(dto, task);
        taskMapper.updateById(task);

        // 2. 更新执行人关联：先删除旧的，再插入新的
        LambdaQueryWrapper<TaskExecutor> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(TaskExecutor::getTaskId, dto.getId());
        taskExecutorMapper.delete(delWrapper);

        if (dto.getExecutorIds() != null && !dto.getExecutorIds().isEmpty()) {
            for (Long userId : dto.getExecutorIds()) {
                TaskExecutor te = new TaskExecutor();
                te.setTaskId(dto.getId());
                te.setUserId(userId);
                taskExecutorMapper.insert(te);
            }
        }

        // 3. 可选：给新执行人发送通知（这里简化，只更新关联）
        // 如需发送通知，可对比新旧执行人列表，此处略
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        // 删除任务基本信息
        taskMapper.deleteById(id);
        // 删除任务-执行人关联
        LambdaQueryWrapper<TaskExecutor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutor::getTaskId, id);
        taskExecutorMapper.delete(wrapper);
    }

    @Override
    public void updateStatus(TaskStatusDTO dto) {
        Task task = taskMapper.selectById(dto.getId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(dto.getStatus());
        taskMapper.updateById(task);

        String content = "任务状态已更新：" + task.getTitle();
        notificationService.saveNotification(task.getCreatorId(), LoginUserContext.getUserId(), "TASK_STATUS", content, task.getId());
        NotificationMessage message = new NotificationMessage("TASK_STATUS", content, task.getId(), System.currentTimeMillis());
        NotificationWebSocketHandler.sendMessage(task.getCreatorId(), message);
    }

    @Override
    public void assignTask(TaskAssignDTO dto) {
        // 此方法仍用于单个指派，但建议改用多执行人方式。此处保留原逻辑（操作 task_executor 表）
        Task task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        // 清除原有执行人，添加新的单个执行人
        LambdaQueryWrapper<TaskExecutor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutor::getTaskId, dto.getTaskId());
        taskExecutorMapper.delete(wrapper);
        TaskExecutor te = new TaskExecutor();
        te.setTaskId(dto.getTaskId());
        te.setUserId(dto.getExecutorId());
        taskExecutorMapper.insert(te);

        String content = "你被指派了新任务：" + task.getTitle();
        notificationService.saveNotification(dto.getExecutorId(), LoginUserContext.getUserId(), "TASK_ASSIGN", content, task.getId());
        NotificationMessage message = new NotificationMessage("TASK_ASSIGN", content, task.getId(), System.currentTimeMillis());
        NotificationWebSocketHandler.sendMessage(dto.getExecutorId(), message);
    }

    @Override
    public void sendQuestion(Long taskId, Long receiverId, String content) {
        // 原有逻辑不变
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        Long currentUserId = LoginUserContext.getUserId();
        Long roleId = userRoleMapper.getRoleIdByUserId(receiverId);
        if (roleId == null || roleId != 2L) {
            throw new BusinessException("只能向项目经理发送疑问");
        }
        String notificationContent = String.format("任务【%s】收到新疑问：%s", task.getTitle(), content);
        notificationService.saveNotification(receiverId, currentUserId, "QUESTION", notificationContent, taskId);
        NotificationMessage message = new NotificationMessage("QUESTION", notificationContent, taskId, System.currentTimeMillis());
        NotificationWebSocketHandler.sendMessage(receiverId, message);
    }

    @Override
    public void sendReply(Long taskId, Long receiverId, String content) {
        // 原有逻辑不变
        Long currentUserId = LoginUserContext.getUserId();
        boolean isAdmin = userRoleMapper.existsAdminRole(currentUserId);
        Long roleId = userRoleMapper.getRoleIdByUserId(currentUserId);
        boolean isProjectManager = roleId != null && roleId == 2L;
        if (!isAdmin && !isProjectManager) {
            throw new BusinessException("只有项目经理或管理员可以回复疑问");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        String notificationContent = String.format("任务【%s】收到回复：%s", task.getTitle(), content);
        notificationService.saveNotification(receiverId, currentUserId, "REPLY", notificationContent, taskId);
        NotificationMessage message = new NotificationMessage("REPLY", notificationContent, taskId, System.currentTimeMillis());
        NotificationWebSocketHandler.sendMessage(receiverId, message);
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> map = new HashMap<>();
        Long total = taskMapper.selectCount(null);
        LambdaQueryWrapper<Task> todoWrapper = new LambdaQueryWrapper<>();
        todoWrapper.eq(Task::getStatus, 0);
        Long todo = taskMapper.selectCount(todoWrapper);
        LambdaQueryWrapper<Task> doingWrapper = new LambdaQueryWrapper<>();
        doingWrapper.eq(Task::getStatus, 1);
        Long doing = taskMapper.selectCount(doingWrapper);
        LambdaQueryWrapper<Task> doneWrapper = new LambdaQueryWrapper<>();
        doneWrapper.eq(Task::getStatus, 2);
        Long done = taskMapper.selectCount(doneWrapper);
        map.put("total", total);
        map.put("todo", todo);
        map.put("doing", doing);
        map.put("done", done);
        return map;
    }

    @Override
    @Transactional
    public void markTaskRead(Long taskId) {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) return;
        LambdaQueryWrapper<TaskReadStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskReadStatus::getUserId, userId).eq(TaskReadStatus::getTaskId, taskId);
        TaskReadStatus record = taskReadStatusMapper.selectOne(wrapper);
        if (record == null) {
            record = new TaskReadStatus();
            record.setUserId(userId);
            record.setTaskId(taskId);
            record.setLastReadTime(LocalDateTime.now());
            taskReadStatusMapper.insert(record);
        } else {
            record.setLastReadTime(LocalDateTime.now());
            taskReadStatusMapper.updateById(record);
        }
    }

    /**
     * 封装 TaskVO（支持多执行人）
     */
    private TaskVO buildTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        Project project = projectMapper.selectById(task.getProjectId());
        if (project != null) vo.setProjectName(project.getName());
        User creator = userMapper.selectById(task.getCreatorId());
        if (creator != null) vo.setCreatorName(creator.getNickname());

        // 查询执行人列表
        LambdaQueryWrapper<TaskExecutor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutor::getTaskId, task.getId());
        List<TaskExecutor> executors = taskExecutorMapper.selectList(wrapper);
        List<Long> executorIds = new ArrayList<>();
        List<String> executorNames = new ArrayList<>();
        for (TaskExecutor te : executors) {
            User user = userMapper.selectById(te.getUserId());
            if (user != null) {
                executorIds.add(user.getId());
                executorNames.add(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        vo.setExecutorIds(executorIds);
        vo.setExecutorNames(executorNames);
        vo.setDeadline(task.getEndTime());
        vo.setCreatorId(task.getCreatorId());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }
}