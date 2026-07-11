package com.social.backend.repository;

import com.social.backend.entity.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {

    List<ScheduleParticipant> findByScheduleId(Long scheduleId);

    List<ScheduleParticipant> findByUserId(Long userId);

    void deleteByScheduleId(Long scheduleId);
}