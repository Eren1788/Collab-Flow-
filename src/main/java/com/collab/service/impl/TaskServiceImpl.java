package com.collab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.entity.Task;
import com.collab.mapper.TaskMapper;
import com.collab.service.TaskService;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
}
