package com.social.backend.repository;

import com.social.backend.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    int countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<Like> findByPostId(Long postId);

    List<Like> findByUserId(Long userId);
}