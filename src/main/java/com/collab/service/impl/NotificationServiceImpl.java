package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.Notification;
import com.collab.mapper.NotificationMapper;
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

    @Override
    public List<NotificationVO> myNotifications() {

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(
                Notification::getReceiverId,
                LoginUserContext.getUserId()
        );
        wrapper.orderByDesc(Notification::getId);

        List<Notification> list = notificationMapper.selectList(wrapper);

        return list.stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long unreadCount() {

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(
                Notification::getReceiverId,
                LoginUserContext.getUserId()
        );

        wrapper.eq(Notification::getIsRead,0);

        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void read(Long id) {

        Notification notification = notificationMapper.selectById(id);

        if(notification == null){
            return;
        }

        notification.setIsRead(1);

        notificationMapper.updateById(notification);
    }

    @Override
    public void readAll() {

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(
                Notification::getReceiverId,
                LoginUserContext.getUserId()
        );

        List<Notification> list = notificationMapper.selectList(wrapper);

        for (Notification notification : list) {

            notification.setIsRead(1);

            notificationMapper.updateById(notification);
        }
    }

    /**
     * VO转换
     */
    private NotificationVO buildVO(Notification notification){

        NotificationVO vo = new NotificationVO();

        BeanUtils.copyProperties(notification,vo);

        if(notification.getCreateTime() != null){

            vo.setCreateTime(notification.getCreateTime()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "yyyy-MM-dd HH:mm:ss"
                                    )
                            )
            );
        }

        return vo;
    }

    @Override
    public void saveNotification(
            Long receiverId,
            Long senderId,
            String type,
            String content,
            Long businessId
    ) {

        Notification notification = new Notification();

        notification.setReceiverId(receiverId);

        notification.setSenderId(senderId);

        notification.setType(type);

        notification.setContent(content);

        notification.setBusinessId(businessId);

        notification.setIsRead(0);

        notificationMapper.insert(notification);
    }
}