package com.social.backend.repository;

import com.social.backend.entity.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {

    // 获取用户创建的所有群
    List<GroupChat> findByOwnerId(Long ownerId);
}