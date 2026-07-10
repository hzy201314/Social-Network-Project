package com.social.backend.dto;

import java.time.LocalDateTime;

public class GroupMemberResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;  // 0-普通，1-管理员，2-群主
    private LocalDateTime joinedAt;

    // ========== Getters and Setters ==========
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}