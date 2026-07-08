package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.MessageRequest;
import com.social.backend.dto.MessageResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Message;
import com.social.backend.service.MessageService;
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

    // WebSocket 发送消息
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return;
        }

        // 保存消息到数据库
        Message message = messageService.saveMessage(
            currentUser.getId(),
            request.getReceiverId(),
            request.getContent()
        );

        // 转换为响应格式
        MessageResponse response = messageService.convertToResponse(message);

        // 实时推送给接收方（/user/queue/messages）
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getReceiverId()),
            "/queue/messages",
            response
        );

        // 同时回传给发送方（可选，便于前端更新）
        messagingTemplate.convertAndSendToUser(
            String.valueOf(currentUser.getId()),
            "/queue/messages",
            response
        );
    }

    // 获取聊天记录（HTTP 接口）
    @GetMapping("/api/messages/{friendId}")
    @ResponseBody
    public ApiResponse<List<MessageResponse>> getConversation(@PathVariable Long friendId, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<MessageResponse> messages = messageService.getConversation(currentUser.getId(), friendId);
            // 标记为已读
            messageService.markAsRead(currentUser.getId(), friendId);
            return ApiResponse.success(messages);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取未读消息数
    @GetMapping("/api/messages/unread/count")
    @ResponseBody
    public ApiResponse<Integer> getUnreadCount(HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            int count = messageService.getUnreadCount(currentUser.getId());
            return ApiResponse.success(count);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}