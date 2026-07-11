package com.social.backend.dto;

import java.util.List;

public class CreateGroupRequest {
    private String name;
    private String avatar;
    private String announcement;
    private List<Long> memberIds;  // 邀请的成员ID列表（不包括创建者自己）

    // ========== Getters and Setters ==========
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }

    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
}