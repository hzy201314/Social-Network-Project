package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.FriendRequest;
import com.social.backend.dto.FriendRequestResponse;
import com.social.backend.dto.FriendResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.User;
import com.social.backend.service.FriendService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FriendController {

    @Autowired
    private FriendService friendService;

    // 搜索用户
    @GetMapping("/users/search")
    public ApiResponse<List<User>> searchUsers(@RequestParam String keyword, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<User> users = friendService.searchUsers(keyword);
            return ApiResponse.success(users);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 发送好友请求
    @PostMapping("/friends/requests")
    public ApiResponse<Void> sendFriendRequest(@RequestBody FriendRequest request, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.sendFriendRequest(currentUser.getId(), request.getFriendId());
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取好友请求列表（收到的待确认请求）
    @GetMapping("/friends/requests")
    public ApiResponse<List<FriendRequestResponse>> getPendingRequests(HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<FriendRequestResponse> requests = friendService.getPendingRequests(currentUser.getId());
            return ApiResponse.success(requests);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 处理好友请求
    @PutMapping("/friends/requests/{requestId}")
    public ApiResponse<Void> handleFriendRequest(@PathVariable Long requestId, @RequestBody FriendRequest request, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.handleFriendRequest(requestId, currentUser.getId(), request.getStatus());
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取好友列表
    @GetMapping("/friends")
    public ApiResponse<List<FriendResponse>> getFriendList(HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<FriendResponse> friends = friendService.getFriendList(currentUser.getId());
            return ApiResponse.success(friends);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 删除好友
    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendId, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            friendService.deleteFriend(currentUser.getId(), friendId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}