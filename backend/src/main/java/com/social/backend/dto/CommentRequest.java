package com.social.backend.dto;

public class CommentRequest {
    private String content;
    private Long parentId;  // ✅ 新增：父评论ID（回复时传入）

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}