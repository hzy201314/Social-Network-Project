package com.social.backend.repository;

import com.social.backend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    // 查询两个用户之间的好友关系
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);

    // 查询用户的所有好友（状态为已确认）
    List<Friend> findByUserIdAndStatus(Long userId, Integer status);

    // 查询用户收到的待确认请求（friend_id是当前用户，status=0）
    List<Friend> findByFriendIdAndStatus(Long friendId, Integer status);

    // 删除好友关系
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}