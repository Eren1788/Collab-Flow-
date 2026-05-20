package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 我的通知
     */
    @GetMapping("/my")
    public Result<?> myNotifications(){

        return Result.success(
                notificationService.myNotifications()
        );
    }

    /**
     * 未读数量
     */
    @GetMapping("/unread/count")
    public Result<?> unreadCount(){

        return Result.success(
                notificationService.unreadCount()
        );
    }

    /**
     * 已读
     */
    @PutMapping("/read/{id}")
    public Result<?> read(@PathVariable Long id){

        notificationService.read(id);

        return Result.success();
    }

    /**
     * 全部已读
     */
    @PutMapping("/read/all")
    public Result<?> readAll(){

        notificationService.readAll();

        return Result.success();
    }
}