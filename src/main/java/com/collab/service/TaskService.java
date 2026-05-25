package com.collab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.dto.TaskAssignDTO;
import com.collab.dto.TaskDTO;
import com.collab.dto.TaskStatusDTO;
import com.collab.entity.Task;
import com.collab.vo.TaskVO;

import java.util.List;
import java.util.Map;

public interface TaskService extends IService<Task> {

    void addTask(TaskDTO dto);

    List<TaskVO> listTask();

    Page<TaskVO> pageTask(
            Integer pageNum,
            Integer pageSize,
            Long projectId,
            Integer status,
            Long executorId,
            Integer priority,
            String keyword
    );

    TaskVO detail(Long id);

    void updateTask(TaskDTO dto);

    void deleteTask(Long id);

    void updateStatus(TaskStatusDTO dto);

    void assignTask(TaskAssignDTO dto);

    Map<String,Object> statistics();

    /**
     * 发送疑问（普通成员向项目经理/管理员提问）
     * @param taskId 任务ID
     * @param content 疑问内容
     */
    void sendQuestion(Long taskId, Long receiverId, String content);

    /**
     * 回复疑问（项目经理/管理员回复提问者）
     * @param taskId 任务ID
     * @param receiverId 接收人ID（提问者）
     * @param content 回复内容
     */
    void sendReply(Long taskId, Long receiverId, String content);

    void markTaskRead(Long taskId);
}