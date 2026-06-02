package com.collab.service;

import com.collab.vo.NotificationVO;

import java.util.List;

public interface NotificationService {

    /**
     * 我的通知
     */
    List<NotificationVO> myNotifications();

    /**
     * 未读数量
     */
    Long unreadCount();

    /**
     * 已读
     */
    void read(Long id);

    /**
     * 全部已读
     */
    void readAll();

    /**
     * 保存通知
     */
    void saveNotification(
            Long receiverId,
            Long senderId,
            String type,
            String content,
            Long businessId
    );

    /**
     * 批量删除通知（仅限自己的）
     */
    void deleteNotifications(List<Long> ids);
}