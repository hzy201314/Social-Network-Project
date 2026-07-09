package com.social.backend.service;

import com.social.backend.dto.FriendRequestResponse;
import com.social.backend.dto.FriendResponse;
import com.social.backend.entity.Friend;
import com.social.backend.entity.User;
import com.social.backend.repository.FriendRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_REJECTED = 2;

    // ===== 搜索用户（排除自己，返回好友状态） =====
    public List<Map<String, Object>> searchUsers(String keyword, String field, Long currentUserId) {
        if (keyword == null || keyword.isEmpty()) {
            throw new RuntimeException("搜索关键词不能为空");
        }

        List<User> results;
        if (field == null || field.isEmpty() || "all".equals(field)) {
            results = userRepository.searchUsers(keyword);
        } else {
            results = userRepository.searchUsersByField(keyword, field);
        }

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (User user : results) {
            // 排除自己
            if (user.getId().equals(currentUserId)) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("avatar", user.getAvatar());

            // 检查好友关系状态
            Optional<Friend> existing = friendRepository.findByUserIdAndFriendId(currentUserId, user.getId());
            if (existing.isPresent()) {
                int status = existing.get().getStatus();
                if (status == STATUS_ACCEPTED) {
                    item.put("friendStatus", "好友");
                } else if (status == STATUS_PENDING) {
                    item.put("friendStatus", "待确认");
                } else {
                    item.put("friendStatus", "已拒绝");
                }
            } else {
                // 检查对方是否向我发过请求
                Optional<Friend> reverse = friendRepository.findByUserIdAndFriendId(user.getId(), currentUserId);
                if (reverse.isPresent() && reverse.get().getStatus() == STATUS_PENDING) {
                    item.put("friendStatus", "待处理");
                } else {
                    item.put("friendStatus", "未添加");
                }
            }
            responseList.add(item);
        }
        return responseList;
    }

    // ===== 检查两个用户是否为好友 =====
    public boolean areFriends(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            return false;
        }

        Optional<Friend> friend = friendRepository.findByUserIdAndFriendId(userId1, userId2);
        return friend.isPresent() && friend.get().getStatus() == STATUS_ACCEPTED;
    }

    // ===== 检查是否为好友（用于Controller返回友好信息） =====
    public void checkFriendship(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new RuntimeException("不能给自己发消息");
        }

        if (!areFriends(userId1, userId2)) {
            throw new RuntimeException("还不是好友，无法发送消息");
        }
    }

    // 发送好友请求
    @Transactional
    public void sendFriendRequest(Long userId, Long friendId) {
        // 自己不能加自己（已有）
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查对方是否存在
        if (!userRepository.existsById(friendId)) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否已经是好友或已有请求
        Friend existing = friendRepository.findByUserIdAndFriendId(userId, friendId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == STATUS_PENDING) {
                throw new RuntimeException("已发送好友请求，等待对方确认");
            } else if (existing.getStatus() == STATUS_ACCEPTED) {
                throw new RuntimeException("已经是好友");
            }
        }

        // 检查是否有反向的待确认请求（对方先加了你）
        Friend reverse = friendRepository.findByUserIdAndFriendId(friendId, userId).orElse(null);
        if (reverse != null && reverse.getStatus() == STATUS_PENDING) {
            // 自动同意：对方先申请，现在你申请，直接成为好友
            reverse.setStatus(STATUS_ACCEPTED);
            reverse.setUpdatedAt(LocalDateTime.now());
            friendRepository.save(reverse);
            return;
        }

        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(STATUS_PENDING);
        friend.setCreatedAt(LocalDateTime.now());
        friend.setUpdatedAt(LocalDateTime.now());
        friendRepository.save(friend);
    }

    // 获取好友请求列表（收到的待确认请求）
    public List<FriendRequestResponse> getPendingRequests(Long userId) {
        List<Friend> pendingList = friendRepository.findByFriendIdAndStatus(userId, STATUS_PENDING);
        return pendingList.stream().map(f -> {
            User user = userRepository.findById(f.getUserId()).orElse(null);
            FriendRequestResponse resp = new FriendRequestResponse();
            resp.setId(f.getId());
            resp.setUserId(f.getUserId());
            if (user != null) {
                resp.setUsername(user.getUsername());
                resp.setNickname(user.getNickname());
                resp.setAvatar(user.getAvatar());
            }
            resp.setStatus(f.getStatus());
            resp.setCreatedAt(f.getCreatedAt());
            return resp;
        }).collect(Collectors.toList());
    }

    // 处理好友请求（同意/拒绝）
    @Transactional
    public void handleFriendRequest(Long requestId, Long userId, Integer action) {
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("好友请求不存在"));

        // 只有请求的接收方才能处理
        if (!friend.getFriendId().equals(userId)) {
            throw new RuntimeException("没有权限处理此请求");
        }

        if (friend.getStatus() != STATUS_PENDING) {
            throw new RuntimeException("该请求已被处理");
        }

        if (action == STATUS_ACCEPTED) {
            friend.setStatus(STATUS_ACCEPTED);
        } else if (action == STATUS_REJECTED) {
            friend.setStatus(STATUS_REJECTED);
        } else {
            throw new RuntimeException("无效的操作");
        }
        friend.setUpdatedAt(LocalDateTime.now());
        friendRepository.save(friend);
    }

    // 获取好友列表
    public List<FriendResponse> getFriendList(Long userId) {
        List<Friend> friendList = friendRepository.findByUserIdAndStatus(userId, STATUS_ACCEPTED);
        return friendList.stream().map(f -> {
            User friendUser = userRepository.findById(f.getFriendId()).orElse(null);
            FriendResponse resp = new FriendResponse();
            if (friendUser != null) {
                resp.setId(friendUser.getId());
                resp.setUsername(friendUser.getUsername());
                resp.setNickname(friendUser.getNickname());
                resp.setAvatar(friendUser.getAvatar());
            }
            resp.setStatus(STATUS_ACCEPTED);
            return resp;
        }).collect(Collectors.toList());
    }

    // 删除好友
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        friendRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendRepository.deleteByUserIdAndFriendId(friendId, userId);
    }
}