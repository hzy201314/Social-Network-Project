package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.MessageRequest;
import com.social.backend.dto.MessageResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Message;
import com.social.backend.service.FriendService;
import com.social.backend.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private FriendService friendService;   // ✅ 新增

    // WebSocket 发送消息
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return;
        }

        // ✅ 检查是否为好友
        try {
            friendService.checkFriendship(userId, request.getReceiverId());
        } catch (RuntimeException e) {
            System.out.println("❌ 非好友不能发送消息: " + userId + " -> " + request.getReceiverId());
            // 可以通过 WebSocket 返回错误，这里先打印日志
            return;
        }

        // 保存消息到数据库
        Message message = messageService.saveMessage(
            userId,
            request.getReceiverId(),
            request.getContent()
        );

        // 转换为响应格式
        MessageResponse response = messageService.convertToResponse(message);

        // 实时推送给接收方
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getReceiverId()),
            "/queue/messages",
            response
        );

        // 同时回传给发送方
        messagingTemplate.convertAndSendToUser(
            String.valueOf(userId),
            "/queue/messages",
            response
        );
    }

    // 获取聊天记录（HTTP 接口）
    @GetMapping("/api/messages/{friendId}")
    @ResponseBody
    public ApiResponse<List<MessageResponse>> getConversation(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        // ✅ 检查是否为好友
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

    // 获取未读消息数
    @GetMapping("/api/messages/unread/count")
    @ResponseBody
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
}