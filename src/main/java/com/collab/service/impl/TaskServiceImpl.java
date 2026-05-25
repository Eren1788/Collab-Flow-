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
import org.springframework.util.StringUtils;

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

    @Override
    public void addTask(TaskDTO dto) {

        Task task = new Task();

        BeanUtils.copyProperties(dto, task);

        task.setCreatorId(LoginUserContext.getUserId());

        taskMapper.insert(task);

        /*
         * =========================
         * 任务创建动态
         * =========================
         */
        ProjectActivity activity = new ProjectActivity();

        activity.setProjectId(task.getProjectId());

        activity.setUserId(LoginUserContext.getUserId());

        activity.setType("TASK_CREATE");

        activity.setContent("创建了任务：" + task.getTitle());

        projectActivityService.addActivity(activity);

        /**
         * 新任务通知执行人
         */
        if (task.getExecutorId() != null) {

            String content =
                    "你有一个新的任务：" + task.getTitle();
            /**
             * 1、保存数据库通知
             */
            notificationService.saveNotification(
                    task.getExecutorId(),
                    LoginUserContext.getUserId(),
                    "TASK_CREATE",
                    content,
                    task.getId()
            );

            /**
             * 2、WebSocket实时推送
             */
            NotificationMessage message =
                    new NotificationMessage(
                            "TASK_CREATE",
                            content,
                            task.getId(),
                            System.currentTimeMillis()
                    );

            NotificationWebSocketHandler.sendMessage(
                    task.getExecutorId(),
                    message
            );
        }
    }

    @Override
    public List<TaskVO> listTask() {

        List<Task> tasks = taskMapper.selectList(null);

        return tasks.stream()
                .map(this::buildTaskVO)
                .collect(Collectors.toList());
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

        Page<Task> page = new Page<>(pageNum,pageSize);

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        //添加判断
        if (projectId != null) {
            wrapper.eq(Task::getProjectId, projectId);
        }

        wrapper.eq(
                projectId != null,
                Task::getProjectId,
                projectId
        );

        wrapper.eq(
                status != null,
                Task::getStatus,
                status
        );

        wrapper.eq(
                executorId != null,
                Task::getExecutorId,
                executorId
        );

        wrapper.eq(
                priority != null,
                Task::getPriority,
                priority
        );

        wrapper.like(
                StringUtils.hasText(keyword),
                Task::getTitle,
                keyword
        );

        wrapper.orderByDesc(Task::getId);

        Page<Task> taskPage = taskMapper.selectPage(page,wrapper);

        Page<TaskVO> result = new Page<>();

        BeanUtils.copyProperties(taskPage,result);

        List<TaskVO> records = taskPage.getRecords()
                        .stream()
                        .map(this::buildTaskVO)
                        .collect(Collectors.toList());

        result.setRecords(records);

        return result;
    }

    @Override
    public TaskVO detail(Long id) {
        Task task = taskMapper.selectById(id);
        return buildTaskVO(task);
    }

    @Override
    public void updateTask(TaskDTO dto) {
        Task task = new Task();
        BeanUtils.copyProperties(dto,task);
        taskMapper.updateById(task);
    }

    @Override
    public void deleteTask(Long id) {
        taskMapper.deleteById(id);
    }

    @Override
    public void updateStatus(TaskStatusDTO dto) {

        Task task = taskMapper.selectById(dto.getId());

        if(task == null){
            throw new BusinessException("任务不存在");
        }

        task.setStatus(dto.getStatus());

        taskMapper.updateById(task);

        String content =
                "任务状态已更新：" + task.getTitle();

        /**
         * 1、保存通知
         */
        notificationService.saveNotification(
                task.getCreatorId(),
                LoginUserContext.getUserId(),
                "TASK_STATUS",
                content,
                task.getId()
        );

        /**
         * 2、WebSocket推送
         */
        NotificationMessage message =
                new NotificationMessage(
                        "TASK_STATUS",
                        content,
                        task.getId(),
                        System.currentTimeMillis()
                );

        NotificationWebSocketHandler.sendMessage(
                task.getCreatorId(),
                message
        );
    }

    @Override
    public void assignTask(TaskAssignDTO dto) {

        Task task = taskMapper.selectById(dto.getTaskId());

        if(task == null){
            throw new BusinessException("任务不存在");
        }

        task.setExecutorId(dto.getExecutorId());

        taskMapper.updateById(task);

        String content = "你被指派了新任务：" + task.getTitle();

        /**
         * 1、保存通知
         */
        notificationService.saveNotification(
                dto.getExecutorId(),
                LoginUserContext.getUserId(),
                "TASK_ASSIGN",
                content,
                task.getId()
        );

        /**
         * 2、WebSocket推送
         */
        NotificationMessage message =
                new NotificationMessage(
                        "TASK_ASSIGN",
                        content,
                        task.getId(),
                        System.currentTimeMillis()
                );

        NotificationWebSocketHandler.sendMessage(
                dto.getExecutorId(),
                message
        );
    }

    /**
     * 发送疑问（修改为指定接收人）
     * @param taskId 任务ID
     * @param receiverId 接收人ID（必须是项目经理）
     * @param content 疑问内容
     */
    @Override
    public void sendQuestion(Long taskId, Long receiverId, String content) {
        // 1. 获取任务信息
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        Long currentUserId = LoginUserContext.getUserId();

        // 2. 校验接收人是否为项目经理（roleId = 2）或超级管理员（可选，根据需求可只允许项目经理）
        Long roleId = userRoleMapper.getRoleIdByUserId(receiverId);
        if (roleId == null || roleId != 2L) {
            throw new BusinessException("只能向项目经理发送疑问");
        }

        // 3. 创建通知
        String notificationContent = String.format("任务【%s】收到新疑问：%s", task.getTitle(), content);
        notificationService.saveNotification(
                receiverId,
                currentUserId,
                "QUESTION",
                notificationContent,
                taskId
        );

        // 4. WebSocket 实时推送
        NotificationMessage message = new NotificationMessage(
                "QUESTION",
                notificationContent,
                taskId,
                System.currentTimeMillis()
        );
        NotificationWebSocketHandler.sendMessage(receiverId, message);
    }

    @Override
    public void sendReply(Long taskId, Long receiverId, String content) {
        // 1. 校验权限：只有项目经理或超级管理员可以回复
        Long currentUserId = LoginUserContext.getUserId();
        boolean isAdmin = userRoleMapper.existsAdminRole(currentUserId);
        // 判断是否为项目经理（roleId=2）
        Long roleId = userRoleMapper.getRoleIdByUserId(currentUserId);
        boolean isProjectManager = roleId != null && roleId == 2L;

        if (!isAdmin && !isProjectManager) {
            throw new BusinessException("只有项目经理或管理员可以回复疑问");
        }

        // 2. 获取任务信息
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 3. 创建回复通知
        String notificationContent = String.format("任务【%s】收到回复：%s", task.getTitle(), content);
        notificationService.saveNotification(
                receiverId,
                currentUserId,
                "REPLY",
                notificationContent,
                taskId
        );

        // WebSocket 实时推送
        NotificationMessage message = new NotificationMessage(
                "REPLY",
                notificationContent,
                taskId,
                System.currentTimeMillis()
        );
        NotificationWebSocketHandler.sendMessage(receiverId, message);
    }

    @Override
    public Map<String, Object> statistics() {

        Map<String,Object> map = new HashMap<>();

        Long total = taskMapper.selectCount(null);

        LambdaQueryWrapper<Task> todoWrapper = new LambdaQueryWrapper<>();

        todoWrapper.eq(Task::getStatus,0);

        Long todo = taskMapper.selectCount(todoWrapper);

        LambdaQueryWrapper<Task> doingWrapper = new LambdaQueryWrapper<>();

        doingWrapper.eq(Task::getStatus,1);

        Long doing = taskMapper.selectCount(doingWrapper);

        LambdaQueryWrapper<Task> doneWrapper = new LambdaQueryWrapper<>();

        doneWrapper.eq(Task::getStatus,2);

        Long done = taskMapper.selectCount(doneWrapper);

        map.put("total",total);

        map.put("todo",todo);

        map.put("doing",doing);

        map.put("done",done);

        return map;
    }

    /**
     * 封装 TaskVO
     */
    private TaskVO buildTaskVO(Task task){

        TaskVO vo = new TaskVO();

        BeanUtils.copyProperties(task,vo);

        Project project = projectMapper.selectById(task.getProjectId());

        if(project != null){
            vo.setProjectName(project.getName());
        }

        User creator = userMapper.selectById(task.getCreatorId());

        if(creator != null){

            vo.setCreatorName(creator.getNickname());
        }

        User executor = userMapper.selectById(task.getExecutorId());

        if(executor != null){

            vo.setExecutorName(executor.getNickname());
        }

        vo.setDeadline(task.getEndTime());

        vo.setCreatorId(task.getCreatorId());

        vo.setExecutorId(task.getExecutorId());

        vo.setCreateTime(task.getCreateTime());

        return vo;
    }
}