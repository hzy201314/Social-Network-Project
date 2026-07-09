package com.social.backend.repository;

import com.social.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    // 按用户名模糊搜索（保留，兼容旧代码）
    List<User> findByUsernameContaining(String keyword);
    
    //新增：多字段模糊搜索（用户名、昵称、邮箱、ID）
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(u.id AS string) LIKE CONCAT('%', :keyword, '%')")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    // 新增：按指定字段搜索（支持前端传参）
    @Query("SELECT u FROM User u WHERE " +
           "(:field = 'username' AND LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:field = 'nickname' AND LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:field = 'email' AND LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:field = 'id' AND CAST(u.id AS string) LIKE CONCAT('%', :keyword, '%')) OR " +
           "(:field = 'all' AND (" +
           "   LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "   LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "   LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "   CAST(u.id AS string) LIKE CONCAT('%', :keyword, '%')" +
           "))")
    List<User> searchUsersByField(@Param("keyword") String keyword, 
                                  @Param("field") String field);
}