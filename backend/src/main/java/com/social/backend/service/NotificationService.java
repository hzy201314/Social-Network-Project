package com.social.backend.service;

import com.social.backend.dto.NotificationResponse;
import com.social.backend.entity.Notification;
import com.social.backend.entity.User;
import com.social.backend.repository.NotificationRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // 发送通知
    public void sendNotification(Long userId, Long senderId, String type, Long targetId, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetId(targetId);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        System.out.println("📬 通知已发送: " + userId + " -> " + content);
    }

    // 获取用户未读通知
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, 0);
        return notifications.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 获取用户所有通知
    public List<NotificationResponse> getAllNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 获取未读通知数
    public int getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, 0);
    }

    // 标记单条通知为已读
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在"));
        if (notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notificationRepository.save(notification);
        }
    }

    // 标记所有通知为已读
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, 0);
        for (Notification notification : notifications) {
            notification.setIsRead(1);
            notificationRepository.save(notification);
        }
    }

    // 转换方法
    private NotificationResponse convertToResponse(Notification notification) {
        User sender = userRepository.findById(notification.getSenderId()).orElse(null);
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setSenderId(notification.getSenderId());
        response.setType(notification.getType());
        response.setTargetId(notification.getTargetId());
        response.setContent(notification.getContent());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());

        if (sender != null) {
            response.setSenderUsername(sender.getUsername());
            response.setSenderNickname(sender.getNickname());
            response.setSenderAvatar(sender.getAvatar());
        }
        return response;
    }
}