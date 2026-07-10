package com.social.backend.service;

import com.social.backend.dto.*;
import com.social.backend.entity.GroupChat;
import com.social.backend.entity.GroupMember;
import com.social.backend.entity.GroupMessage;
import com.social.backend.entity.User;
import com.social.backend.repository.GroupChatRepository;
import com.social.backend.repository.GroupMemberRepository;
import com.social.backend.repository.GroupMessageRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupChatRepository groupChatRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendService friendService;

    private static final int ROLE_NORMAL = 0;
    private static final int ROLE_ADMIN = 1;
    private static final int ROLE_OWNER = 2;

    // ===== 创建群聊 =====
    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查群名
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new RuntimeException("群名称不能为空");
        }

        // 创建群
        GroupChat group = new GroupChat();
        group.setName(request.getName());
        group.setAvatar(request.getAvatar());
        group.setOwnerId(userId);
        group.setAnnouncement(request.getAnnouncement());
        group.setMemberCount(1);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        GroupChat savedGroup = groupChatRepository.save(group);

        // 添加创建者为群主
        GroupMember owner = new GroupMember();
        owner.setGroupId(savedGroup.getId());
        owner.setUserId(userId);
        owner.setRole(ROLE_OWNER);
        owner.setJoinedAt(LocalDateTime.now());
        groupMemberRepository.save(owner);

        // 添加邀请的成员（必须为好友）
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (Long memberId : request.getMemberIds()) {
                // 检查是否为好友
                if (!friendService.areFriends(userId, memberId)) {
                    throw new RuntimeException("只能邀请好友入群: " + memberId);
                }
                // 不能重复邀请
                if (memberId.equals(userId)) continue;

                GroupMember member = new GroupMember();
                member.setGroupId(savedGroup.getId());
                member.setUserId(memberId);
                member.setRole(ROLE_NORMAL);
                member.setJoinedAt(LocalDateTime.now());
                groupMemberRepository.save(member);
                savedGroup.setMemberCount(savedGroup.getMemberCount() + 1);
            }
            groupChatRepository.save(savedGroup);
        }

        return convertToResponse(savedGroup, userId);
    }

    // ===== 获取用户的所有群聊 =====
    public List<GroupResponse> getUserGroups(Long userId) {
        List<Long> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        List<GroupChat> groups = new ArrayList<>();
        for (Long groupId : groupIds) {
            groupChatRepository.findById(groupId).ifPresent(groups::add);
        }
        return groups.stream()
                .map(group -> convertToResponse(group, userId))
                .collect(Collectors.toList());
    }

    // ===== 获取群详情 =====
    public GroupResponse getGroupDetail(Long groupId, Long userId) {
        // 检查用户是否在群中
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("您不在该群中"));

        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));

        return convertToResponse(group, userId);
    }

    // ===== 获取群成员列表 =====
    public List<GroupMemberResponse> getGroupMembers(Long groupId, Long userId) {
        // 检查用户是否在群中
        if (!groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new RuntimeException("您不在该群中");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream()
                .map(this::convertToMemberResponse)
                .collect(Collectors.toList());
    }

    // ===== 退出群聊 =====
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("您不在该群中"));

        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));

        // 群主不能直接退出，需要先转让或解散
        if (member.getRole() == ROLE_OWNER) {
            throw new RuntimeException("群主不能直接退出，请先解散群聊或转让群主");
        }

        groupMemberRepository.delete(member);
        group.setMemberCount(group.getMemberCount() - 1);
        groupChatRepository.save(group);
    }

    // ===== 解散群聊 =====
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));

        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主可以解散群聊");
        }

        // 删除所有成员（CASCADE自动处理）
        groupMemberRepository.deleteAll(groupMemberRepository.findByGroupId(groupId));
        groupChatRepository.delete(group);
    }

    // ===== 更新群信息（名称、头像、公告） =====
    @Transactional
    public GroupResponse updateGroup(Long groupId, Long userId, String name, String avatar, String announcement) {
        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));

        // 只有群主可以修改
        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("只有群主可以修改群信息");
        }

        if (name != null && !name.isEmpty()) {
            group.setName(name);
        }
        if (avatar != null && !avatar.isEmpty()) {
            group.setAvatar(avatar);
        }
        if (announcement != null) {
            group.setAnnouncement(announcement);
        }
        group.setUpdatedAt(LocalDateTime.now());

        GroupChat updated = groupChatRepository.save(group);
        return convertToResponse(updated, userId);
    }

    // ===== 发送群消息 =====
    @Transactional
    public GroupMessageResponse sendGroupMessage(Long groupId, Long userId, GroupMessageRequest request) {
        // 检查用户是否在群中
        if (!groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new RuntimeException("您不在该群中");
        }

        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("群不存在"));

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(userId);
        message.setContent(request.getContent() != null ? request.getContent() : "");
        message.setFileUrl(request.getFileUrl());
        message.setFileType(request.getFileType());
        message.setFileName(request.getFileName());
        message.setCreatedAt(LocalDateTime.now());

        GroupMessage saved = groupMessageRepository.save(message);
        return convertToMessageResponse(saved);
    }

    // ===== 获取群消息 =====
    public List<GroupMessageResponse> getGroupMessages(Long groupId, Long userId) {
        // 检查用户是否在群中
        if (!groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new RuntimeException("您不在该群中");
        }

        List<GroupMessage> messages = groupMessageRepository.findByGroupIdOrderByCreatedAtAsc(groupId);
        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    // ===== 转换方法 =====
    private GroupResponse convertToResponse(GroupChat group, Long currentUserId) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setAvatar(group.getAvatar());
        response.setOwnerId(group.getOwnerId());
        response.setAnnouncement(group.getAnnouncement());
        response.setMemberCount(group.getMemberCount());
        response.setCreatedAt(group.getCreatedAt());
        response.setUpdatedAt(group.getUpdatedAt());

        // 获取群主信息
        User owner = userRepository.findById(group.getOwnerId()).orElse(null);
        if (owner != null) {
            response.setOwnerUsername(owner.getUsername());
            response.setOwnerNickname(owner.getNickname());
        }

        // 获取当前用户角色
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), currentUserId).orElse(null);
        if (member != null) {
            response.setRole(member.getRole());
        }

        // 获取成员列表
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::convertToMemberResponse)
                .collect(Collectors.toList());
        response.setMembers(memberResponses);

        return response;
    }

    private GroupMemberResponse convertToMemberResponse(GroupMember member) {
        User user = userRepository.findById(member.getUserId()).orElse(null);

        GroupMemberResponse response = new GroupMemberResponse();
        response.setUserId(member.getUserId());
        response.setRole(member.getRole());
        response.setJoinedAt(member.getJoinedAt());

        if (user != null) {
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }

    private GroupMessageResponse convertToMessageResponse(GroupMessage message) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);

        GroupMessageResponse response = new GroupMessageResponse();
        response.setId(message.getId());
        response.setGroupId(message.getGroupId());
        response.setSenderId(message.getSenderId());
        response.setContent(message.getContent());
        response.setFileUrl(message.getFileUrl());
        response.setFileType(message.getFileType());
        response.setFileName(message.getFileName());
        response.setCreatedAt(message.getCreatedAt());

        if (sender != null) {
            response.setSenderUsername(sender.getUsername());
            response.setSenderNickname(sender.getNickname());
            response.setSenderAvatar(sender.getAvatar());
        }
        return response;
    }
}