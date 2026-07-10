package com.social.backend.dto;

import java.util.List;

public class PostRequest {
    private String title;      // ✅ 新增
    private String content;
    private List<String> images;
    private List<String> tags;  // ✅ 新增

    // ========== Getters and Setters ==========
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}