package com.collab.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.Notification;
import com.collab.entity.Project;
import com.collab.entity.Task;
import com.collab.entity.User;
import com.collab.mapper.NotificationMapper;
import com.collab.mapper.ProjectMapper;
import com.collab.mapper.TaskMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.NotificationService;
import com.collab.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    @Override
    public List<NotificationVO> myNotifications() {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, LoginUserContext.getUserId());
        wrapper.orderByDesc(Notification::getId);
        List<Notification> list = notificationMapper.selectList(wrapper);
        return list.stream().map(this::buildVO).collect(Collectors.toList());
    }

    @Override
    public Long unreadCount() {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, LoginUserContext.getUserId());
        wrapper.eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void read(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) return;
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void readAll() {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, LoginUserContext.getUserId());
        List<Notification> list = notificationMapper.selectList(wrapper);
        for (Notification notification : list) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    private NotificationVO buildVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);

        // 格式化时间
        if (notification.getCreateTime() != null) {
            vo.setCreateTime(notification.getCreateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        // 查询发送人姓名
        if (notification.getSenderId() != null) {
            User sender = userMapper.selectById(notification.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                vo.setAvatar(sender.getAvatar());
            } else {
                vo.setSenderName("未知用户");
            }
        }

        // 根据业务ID（任务ID）查询任务标题和项目名称
        if (notification.getBusinessId() != null) {
            Task task = taskMapper.selectById(notification.getBusinessId());
            if (task != null) {
                vo.setTaskTitle(task.getTitle());
                Project project = projectMapper.selectById(task.getProjectId());
                if (project != null) {
                    vo.setProjectName(project.getName());
                }
            }
        }

        return vo;
    }

    @Override
    public void saveNotification(Long receiverId, Long senderId, String type, String content, Long businessId) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setContent(content);
        notification.setBusinessId(businessId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    @Override
    public void deleteNotifications(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Long currentUserId = LoginUserContext.getUserId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Notification::getId, ids);
        wrapper.eq(Notification::getReceiverId, currentUserId); // 只能删除自己的
        notificationMapper.delete(wrapper);
    }
}