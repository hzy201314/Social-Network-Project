package com.social.backend.dto;

public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private String interestTags;  // ✅ 新增
    private Integer hideLikes;
    private Integer hideComments;
    private Integer hideFriends;
    private Integer hidePosts;

    // ========== Getters and Setters ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getInterestTags() { return interestTags; }
    public void setInterestTags(String interestTags) { this.interestTags = interestTags; }

    public Integer getHideLikes() { return hideLikes; }
    public void setHideLikes(Integer hideLikes) { this.hideLikes = hideLikes; }

    public Integer getHideComments() { return hideComments; }
    public void setHideComments(Integer hideComments) { this.hideComments = hideComments; }

    public Integer getHideFriends() { return hideFriends; }
    public void setHideFriends(Integer hideFriends) { this.hideFriends = hideFriends; }

    public Integer getHidePosts() { return hidePosts; }
    public void setHidePosts(Integer hidePosts) { this.hidePosts = hidePosts; }
}