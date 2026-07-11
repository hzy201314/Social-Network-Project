package com.social.backend.dto;

public class MessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;
    private String fileUrl;   // ✅ 新增
    private String fileType;  // ✅ 新增
    private String fileName;  // ✅ 新增

    // ========== Getters and Setters ==========
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}