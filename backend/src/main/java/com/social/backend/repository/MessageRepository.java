package com.social.backend.repository;

import com.social.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);

    List<Message> findByReceiverIdAndIsRead(Long receiverId, Integer isRead);

    // ✅ 新增：获取未读消息
    @Query("SELECT m FROM Message m WHERE m.receiverId = :userId AND m.isRead = 0 ORDER BY m.createdAt ASC")
    List<Message> findUnreadByReceiverId(@Param("userId") Long userId);
}