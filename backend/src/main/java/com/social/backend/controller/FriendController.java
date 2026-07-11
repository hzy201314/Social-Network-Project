package com.social.backend.controller;

import com.social.backend.dto.*;
import com.social.backend.service.FriendService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FriendController {

    @Autowired
    private FriendService friendService;

    // 搜索用户（返回好友状态，排除自己）
    @GetMapping("/users/search")
    public ApiResponse<List<Map<String, Object>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "all") String field,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<Map<String, Object>> users = friendService.searchUsers(keyword, field, userId);
            return ApiResponse.success(users);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 发送好友请求
    @PostMapping("/friends/requests")
    public ApiResponse<Void> sendFriendRequest(@RequestBody FriendRequest friendRequest, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.sendFriendRequest(userId, friendRequest.getFriendId());
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取好友请求列表
    @GetMapping("/friends/requests")
    public ApiResponse<List<FriendRequestResponse>> getPendingRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<FriendRequestResponse> requests = friendService.getPendingRequests(userId);
            return ApiResponse.success(requests);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 处理好友请求
    @PutMapping("/friends/requests/{requestId}")
    public ApiResponse<Void> handleFriendRequest(@PathVariable Long requestId, @RequestBody FriendRequest friendRequest, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.handleFriendRequest(requestId, userId, friendRequest.getStatus());
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取好友列表
    @GetMapping("/friends")
    public ApiResponse<List<FriendResponse>> getFriendList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<FriendResponse> friends = friendService.getFriendList(userId);
            return ApiResponse.success(friends);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 删除好友
    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.deleteFriend(userId, friendId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}