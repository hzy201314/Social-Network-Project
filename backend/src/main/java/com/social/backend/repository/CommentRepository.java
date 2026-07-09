package com.social.backend.repository;

import com.social.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 获取动态的一级评论
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    // 获取某条评论的所有子评论
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    // 获取动态的所有评论（按时间）
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostId(@Param("postId") Long postId);

    // 新增：获取用户的所有评论（按时间倒序）
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 新增：获取用户评论过的所有动态ID（去重）
    @Query("SELECT DISTINCT c.postId FROM Comment c WHERE c.userId = :userId")
    List<Long> findDistinctPostIdsByUserId(@Param("userId") Long userId);
}