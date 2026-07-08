package com.social.backend.dto;

public class FriendRequest {
    private Long friendId;
    private Integer status;  // 1-同意，2-拒绝

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}