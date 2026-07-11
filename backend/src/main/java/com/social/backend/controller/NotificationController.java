package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.NotificationResponse;
import com.social.backend.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // ===== 获取未读通知列表 =====
    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
            return ApiResponse.success(notifications);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取所有通知列表 =====
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getAllNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<NotificationResponse> notifications = notificationService.getAllNotifications(userId);
            return ApiResponse.success(notifications);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取未读通知数 =====
    @GetMapping("/unread/count")
    public ApiResponse<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            int count = notificationService.getUnreadCount(userId);
            return ApiResponse.success(count);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 标记单条通知为已读 =====
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            notificationService.markAsRead(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 标记所有通知为已读 =====
    @PutMapping("/read/all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            notificationService.markAllAsRead(userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}