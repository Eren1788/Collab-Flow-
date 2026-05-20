package com.collab.service.impl;

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
import com.collab.entity.Project;
import com.collab.entity.Task;
import com.collab.entity.User;
import com.collab.mapper.ProjectMapper;
import com.collab.mapper.TaskMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.TaskService;
import com.collab.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    @Override
    public void addTask(TaskDTO dto) {

        Task task = new Task();

        BeanUtils.copyProperties(dto, task);

        task.setCreatorId(LoginUserContext.getUserId());

        taskMapper.insert(task);

        /**
         * 新任务通知执行人
         */
        if (task.getExecutorId() != null) {
            NotificationMessage message = new NotificationMessage(
                            "TASK_CREATE",
                            "你有一个新的任务：" + task.getTitle(),
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

        /**
         * 通知任务创建人
         */
        NotificationMessage message = new NotificationMessage(
                        "TASK_STATUS",
                        "任务状态已更新：" + task.getTitle(),
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

        /**
         * 通知新执行人
         */
        NotificationMessage message = new NotificationMessage(
                        "TASK_ASSIGN",
                        "你被指派了新任务：" + task.getTitle(),
                        task.getId(),
                        System.currentTimeMillis()
                );
        NotificationWebSocketHandler.sendMessage(
                dto.getExecutorId(),
                message
        );
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

        return vo;
    }
}