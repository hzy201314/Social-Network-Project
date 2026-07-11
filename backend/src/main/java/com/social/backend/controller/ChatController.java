package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.MessageRequest;
import com.social.backend.dto.MessageResponse;
import com.social.backend.entity.Message;
import com.social.backend.service.FriendService;
import com.social.backend.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private FriendService friendService;

    // ===== 发送消息 =====
    @PostMapping("/send")
    public ApiResponse<MessageResponse> sendMessage(@RequestBody MessageRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        // 检查是否为好友
        try {
            friendService.checkFriendship(userId, request.getReceiverId());
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }

        Message message = messageService.saveMessage(
            userId,
            request.getReceiverId(),
            request.getContent()
        );

        MessageResponse response = messageService.convertToResponse(message);
        return ApiResponse.success(response);
    }

    // ===== 获取聊天记录 =====
    @GetMapping("/{friendId}")
    public ApiResponse<List<MessageResponse>> getConversation(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.checkFriendship(userId, friendId);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }

        try {
            List<MessageResponse> messages = messageService.getConversation(userId, friendId);
            messageService.markAsRead(userId, friendId);
            return ApiResponse.success(messages);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取未读消息列表 =====
    @GetMapping("/unread")
    public ApiResponse<List<MessageResponse>> getUnreadMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<MessageResponse> messages = messageService.getUnreadMessages(userId);
            return ApiResponse.success(messages);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取未读消息数 =====
    @GetMapping("/unread/count")
    public ApiResponse<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            int count = messageService.getUnreadCount(userId);
            return ApiResponse.success(count);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 标记消息为已读 =====
    @PutMapping("/read")
    public ApiResponse<Void> markMessagesAsRead(@RequestBody Map<String, List<Long>> payload, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<Long> messageIds = payload.get("messageIds");
            if (messageIds != null && !messageIds.isEmpty()) {
                messageService.markMessagesAsRead(userId, messageIds);
            }
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}