package com.social.backend.service;

import com.social.backend.dto.MessageResponse;
import com.social.backend.entity.Message;
import com.social.backend.entity.User;
import com.social.backend.repository.MessageRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // 保存消息（支持文件）
    public Message saveMessage(Long senderId, Long receiverId, String content,
                               String fileUrl, String fileType, String fileName) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content != null ? content : "");
        message.setFileUrl(fileUrl);
        message.setFileType(fileType);
        message.setFileName(fileName);
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    // 获取聊天记录
    public List<MessageResponse> getConversation(Long userId, Long friendId) {
        List<Message> messages = messageRepository.findConversation(userId, friendId);
        return messages.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 标记消息为已读
    @Transactional
    public void markAsRead(Long userId, Long senderId) {
        List<Message> unreadMessages = messageRepository.findByReceiverIdAndIsRead(userId, 0);
        for (Message msg : unreadMessages) {
            if (msg.getSenderId().equals(senderId)) {
                msg.setIsRead(1);
                messageRepository.save(msg);
            }
        }
    }

    // 标记多条消息为已读
    @Transactional
    public void markMessagesAsRead(Long userId, List<Long> messageIds) {
        for (Long messageId : messageIds) {
            Message message = messageRepository.findById(messageId).orElse(null);
            if (message != null && message.getReceiverId().equals(userId)) {
                message.setIsRead(1);
                messageRepository.save(message);
            }
        }
    }

    // 获取未读消息数
    public int getUnreadCount(Long userId) {
        List<Message> messages = messageRepository.findByReceiverIdAndIsRead(userId, 0);
        return messages != null ? messages.size() : 0;
    }

    // 获取未读消息列表
    public List<MessageResponse> getUnreadMessages(Long userId) {
        List<Message> messages = messageRepository.findUnreadByReceiverId(userId);
        return messages.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 转换方法
    public MessageResponse convertToResponse(Message message) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSenderId(message.getSenderId());
        response.setReceiverId(message.getReceiverId());
        response.setContent(message.getContent());
        response.setFileUrl(message.getFileUrl());
        response.setFileType(message.getFileType());
        response.setFileName(message.getFileName());
        response.setIsRead(message.getIsRead());
        response.setCreatedAt(message.getCreatedAt());

        if (sender != null) {
            response.setSenderUsername(sender.getUsername());
            response.setSenderNickname(sender.getNickname());
            response.setSenderAvatar(sender.getAvatar());
        }
        return response;
    }
}