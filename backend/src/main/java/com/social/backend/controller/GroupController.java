package com.social.backend.controller;

import com.social.backend.dto.*;
import com.social.backend.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    // ===== 创建群聊 =====
    @PostMapping
    public ApiResponse<GroupResponse> createGroup(@RequestBody CreateGroupRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            GroupResponse group = groupService.createGroup(userId, request);
            return ApiResponse.success(group);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取我的群列表 =====
    @GetMapping
    public ApiResponse<List<GroupResponse>> getMyGroups(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<GroupResponse> groups = groupService.getUserGroups(userId);
            return ApiResponse.success(groups);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取群详情 =====
    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroupDetail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            GroupResponse group = groupService.getGroupDetail(id, userId);
            return ApiResponse.success(group);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取群成员列表 =====
    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> getGroupMembers(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<GroupMemberResponse> members = groupService.getGroupMembers(id, userId);
            return ApiResponse.success(members);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 退出群聊 =====
    @DeleteMapping("/{id}/leave")
    public ApiResponse<Void> leaveGroup(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            groupService.leaveGroup(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 解散群聊 =====
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            groupService.deleteGroup(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 发送群消息 =====
    @PostMapping("/{id}/messages")
    public ApiResponse<GroupMessageResponse> sendGroupMessage(
            @PathVariable Long id,
            @RequestBody GroupMessageRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            GroupMessageResponse message = groupService.sendGroupMessage(id, userId, request);
            return ApiResponse.success(message);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取群消息 =====
    @GetMapping("/{id}/messages")
    public ApiResponse<List<GroupMessageResponse>> getGroupMessages(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<GroupMessageResponse> messages = groupService.getGroupMessages(id, userId);
            return ApiResponse.success(messages);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 更新群信息 =====
    @PutMapping("/{id}")
    public ApiResponse<GroupResponse> updateGroup(
            @PathVariable Long id,
            @RequestBody Map<String, String> params,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            String name = params.get("name");
            String avatar = params.get("avatar");
            String announcement = params.get("announcement");
            GroupResponse group = groupService.updateGroup(id, userId, name, avatar, announcement);
            return ApiResponse.success(group);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}