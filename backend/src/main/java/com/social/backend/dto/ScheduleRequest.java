package com.social.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleRequest {
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String color;
    private Integer isAllDay;
    private Integer remindMinutes;
    private List<Long> participantIds;  // 可选：参与人

    // ========== Getters and Setters ==========
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

    public List<Long> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<Long> participantIds) { this.participantIds = participantIds; }
}