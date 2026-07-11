package com.social.backend.repository;

import com.social.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 获取用户所有未读通知
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Integer isRead);

    // 获取用户未读通知数
    int countByUserIdAndIsRead(Long userId, Integer isRead);

    // 获取用户所有通知（分页）
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}