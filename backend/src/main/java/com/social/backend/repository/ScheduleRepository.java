package com.social.backend.repository;

import com.social.backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 获取用户的所有日程
    List<Schedule> findByUserIdOrderByStartTimeAsc(Long userId);

    // ✅ 修改：获取用户在某时间范围内的日程（支持日期查询）
    List<Schedule> findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(
        Long userId, LocalDateTime start, LocalDateTime end
    );

    // 获取用户某天的日程（使用范围查询）
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId AND s.startTime >= :start AND s.startTime <= :end ORDER BY s.startTime ASC")
    List<Schedule> findUserSchedulesByDateRange(
        @Param("userId") Long userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    // 获取用户进行中的日程
    List<Schedule> findByUserIdAndStatusOrderByStartTimeAsc(Long userId, Integer status);
}