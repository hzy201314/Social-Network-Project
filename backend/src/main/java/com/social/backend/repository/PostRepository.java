package com.social.backend.repository;

import com.social.backend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ✅ 分页查询未删除的动态
    Page<Post> findByIsDeleted(Integer isDeleted, Pageable pageable);

    // ✅ 查询指定用户的未删除动态
    List<Post> findByUserIdAndIsDeleted(Long userId, Integer isDeleted);
}