package com.social.backend.repository;

import com.social.backend.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    // 获取群消息（按时间升序，最新20条）
    List<GroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    // 获取群消息（分页）
    @Query("SELECT gm FROM GroupMessage gm WHERE gm.groupId = :groupId ORDER BY gm.createdAt DESC")
    List<GroupMessage> findLatestByGroupId(@Param("groupId") Long groupId);
}