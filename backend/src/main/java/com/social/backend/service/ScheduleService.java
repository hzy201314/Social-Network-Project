package com.social.backend.service;

import com.social.backend.dto.ScheduleRequest;
import com.social.backend.dto.ScheduleResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Schedule;
import com.social.backend.entity.ScheduleParticipant;
import com.social.backend.entity.User;
import com.social.backend.repository.ScheduleParticipantRepository;
import com.social.backend.repository.ScheduleRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    // 创建日程
    @Transactional
    public ScheduleResponse createSchedule(Long userId, ScheduleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setLocation(request.getLocation());
        schedule.setColor(request.getColor() != null ? request.getColor() : "#4CAF50");
        schedule.setIsAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : 0);
        schedule.setRemindMinutes(request.getRemindMinutes() != null ? request.getRemindMinutes() : 30);
        schedule.setStatus(0);
        schedule.setCreatedAt(LocalDateTime.now());
        schedule.setUpdatedAt(LocalDateTime.now());

        Schedule saved = scheduleRepository.save(schedule);

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (Long participantId : request.getParticipantIds()) {
                if (participantId.equals(userId)) continue;
                ScheduleParticipant participant = new ScheduleParticipant();
                participant.setScheduleId(saved.getId());
                participant.setUserId(participantId);
                participant.setStatus(0);
                participant.setCreatedAt(LocalDateTime.now());
                participantRepository.save(participant);
            }
        }

        return convertToResponse(saved);
    }

    // 获取用户所有日程
    public List<ScheduleResponse> getUserSchedules(Long userId) {
        List<Schedule> schedules = scheduleRepository.findByUserIdOrderByStartTimeAsc(userId);
        return schedules.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // ✅ 修改：获取用户某天的日程（使用范围查询）
    public List<ScheduleResponse> getUserSchedulesByDate(Long userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        List<Schedule> schedules = scheduleRepository.findUserSchedulesByDateRange(userId, startOfDay, endOfDay);
        return schedules.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 获取用户某时间范围内的日程
    public List<ScheduleResponse> getUserSchedulesByRange(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Schedule> schedules = scheduleRepository.findByUserIdAndStartTimeBetweenOrderByStartTimeAsc(userId, start, end);
        return schedules.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 获取单条日程详情
    public ScheduleResponse getScheduleById(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));

        if (!schedule.getUserId().equals(userId)) {
            List<ScheduleParticipant> participants = participantRepository.findByScheduleId(scheduleId);
            boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
            if (!isParticipant) {
                throw new RuntimeException("没有权限查看此日程");
            }
        }

        return convertToResponse(schedule);
    }

    // 更新日程
    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, Long userId, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));

        if (!schedule.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限修改此日程");
        }

        if (request.getTitle() != null) schedule.setTitle(request.getTitle());
        if (request.getDescription() != null) schedule.setDescription(request.getDescription());
        if (request.getStartTime() != null) schedule.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) schedule.setEndTime(request.getEndTime());
        if (request.getLocation() != null) schedule.setLocation(request.getLocation());
        if (request.getColor() != null) schedule.setColor(request.getColor());
        if (request.getIsAllDay() != null) schedule.setIsAllDay(request.getIsAllDay());
        if (request.getRemindMinutes() != null) schedule.setRemindMinutes(request.getRemindMinutes());

        schedule.setUpdatedAt(LocalDateTime.now());
        Schedule updated = scheduleRepository.save(schedule);

        if (request.getParticipantIds() != null) {
            participantRepository.deleteByScheduleId(scheduleId);
            for (Long participantId : request.getParticipantIds()) {
                if (participantId.equals(userId)) continue;
                ScheduleParticipant participant = new ScheduleParticipant();
                participant.setScheduleId(scheduleId);
                participant.setUserId(participantId);
                participant.setStatus(0);
                participant.setCreatedAt(LocalDateTime.now());
                participantRepository.save(participant);
            }
        }

        return convertToResponse(updated);
    }

    // 更新日程状态
    @Transactional
    public ScheduleResponse updateScheduleStatus(Long scheduleId, Long userId, Integer status) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));

        if (!schedule.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限修改此日程");
        }

        schedule.setStatus(status);
        schedule.setUpdatedAt(LocalDateTime.now());
        Schedule updated = scheduleRepository.save(schedule);

        return convertToResponse(updated);
    }

    // 删除日程
    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("日程不存在"));

        if (!schedule.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此日程");
        }

        participantRepository.deleteByScheduleId(scheduleId);
        scheduleRepository.delete(schedule);
    }

    // 转换方法
    private ScheduleResponse convertToResponse(Schedule schedule) {
        User user = userRepository.findById(schedule.getUserId()).orElse(null);

        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setUserId(schedule.getUserId());
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setUserNickname(user.getNickname());
        }
        response.setTitle(schedule.getTitle());
        response.setDescription(schedule.getDescription());
        response.setStartTime(schedule.getStartTime());
        response.setEndTime(schedule.getEndTime());
        response.setLocation(schedule.getLocation());
        response.setColor(schedule.getColor());
        response.setIsAllDay(schedule.getIsAllDay());
        response.setRemindMinutes(schedule.getRemindMinutes());
        response.setStatus(schedule.getStatus());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        List<ScheduleParticipant> participants = participantRepository.findByScheduleId(schedule.getId());
        List<UserResponse> participantResponses = new ArrayList<>();
        for (ScheduleParticipant p : participants) {
            User participantUser = userRepository.findById(p.getUserId()).orElse(null);
            if (participantUser != null) {
                UserResponse ur = new UserResponse();
                ur.setId(participantUser.getId());
                ur.setUsername(participantUser.getUsername());
                ur.setNickname(participantUser.getNickname());
                ur.setAvatar(participantUser.getAvatar());
                participantResponses.add(ur);
            }
        }
        response.setParticipants(participantResponses);

        return response;
    }
}