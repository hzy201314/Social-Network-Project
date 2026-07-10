package com.social.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String bio;

    @Column(name = "interest_tags", length = 200)
    private String interestTags;  // ✅ 新增：兴趣标签

    @Column(name = "hide_likes")
    private Integer hideLikes = 0;

    @Column(name = "hide_comments")
    private Integer hideComments = 0;

    @Column(name = "hide_friends")
    private Integer hideFriends = 0;

    @Column(name = "hide_posts")
    private Integer hidePosts = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ========== Getters and Setters ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}