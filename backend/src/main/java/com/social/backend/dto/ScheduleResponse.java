package com.social.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userNickname;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String color;
    private Integer isAllDay;
    private Integer remindMinutes;
    private Integer status;
    private List<UserResponse> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== Getters and Setters ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getIsAllDay() { return isAllDay; }
    public void setIsAllDay(Integer isAllDay) { this.isAllDay = isAllDay; }

    public Integer getRemindMinutes() { return remindMinutes; }
    public void setRemindMinutes(Integer remindMinutes) { this.remindMinutes = remindMinutes; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public List<UserResponse> getParticipants() { return participants; }
    public void setParticipants(List<UserResponse> participants) { this.participants = participants; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}