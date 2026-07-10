package com.social.backend.dto;

public class MessageRequest {
    private Long senderId;      // ✅ 新增：发送者ID
    private Long receiverId;
    private String content;

    // Getters and Setters
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}