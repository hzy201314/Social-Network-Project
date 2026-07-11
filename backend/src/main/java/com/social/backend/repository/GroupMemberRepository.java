package com.social.backend.repository;

import com.social.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // 查询用户在群中的角色
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    // 查询群的所有成员
    List<GroupMember> findByGroupId(Long groupId);

    // 查询用户加入的所有群ID
    @Query("SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId")
    List<Long> findGroupIdsByUserId(@Param("userId") Long userId);

    // 删除群成员
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}