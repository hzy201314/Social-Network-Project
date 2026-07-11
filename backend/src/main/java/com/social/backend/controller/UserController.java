package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.FriendResponse;
import com.social.backend.dto.PostResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Comment;
import com.social.backend.entity.Post;
import com.social.backend.entity.User;
import com.social.backend.service.FriendService;
import com.social.backend.service.PostService;
import com.social.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private FriendService friendService;

    // ===== 获取当前用户资料 =====
    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            UserResponse user = userService.getCurrentUser(userId);
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取指定用户资料 =====
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserResponse> getUserProfile(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            UserResponse user = userService.getCurrentUser(userId);
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取指定用户的动态列表 =====
    @GetMapping("/{userId}/posts")
    public ApiResponse<List<PostResponse>> getUserPosts(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            User targetUser = userService.getUserEntity(userId);
            if (targetUser == null) {
                return ApiResponse.error("用户不存在");
            }
            if (!currentUserId.equals(userId) && targetUser.getHidePosts() != null && targetUser.getHidePosts() == 1) {
                return ApiResponse.error("该用户已隐藏动态列表");
            }

            List<PostResponse> posts = postService.getPostsByUserIdWithUserInfo(userId, currentUserId);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取他人好友列表 =====
    @GetMapping("/{userId}/friends")
    public ApiResponse<List<FriendResponse>> getUserFriends(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            User targetUser = userService.getUserEntity(userId);
            if (targetUser == null) {
                return ApiResponse.error("用户不存在");
            }
            if (!currentUserId.equals(userId) && targetUser.getHideFriends() != null && targetUser.getHideFriends() == 1) {
                return ApiResponse.error("该用户已隐藏好友列表");
            }

            List<FriendResponse> friends = friendService.getFriendList(userId);
            return ApiResponse.success(friends);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取他人评论列表 =====
    @GetMapping("/{userId}/comments")
    public ApiResponse<List<Comment>> getUserComments(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            User targetUser = userService.getUserEntity(userId);
            if (targetUser == null) {
                return ApiResponse.error("用户不存在");
            }
            if (!currentUserId.equals(userId) && targetUser.getHideComments() != null && targetUser.getHideComments() == 1) {
                return ApiResponse.error("该用户已隐藏评论列表");
            }

            List<Comment> comments = postService.getCommentsByUser(userId);
            return ApiResponse.success(comments);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取他人赞过的动态列表（带用户信息） =====
    @GetMapping("/{userId}/likes")
    public ApiResponse<List<PostResponse>> getUserLikedPosts(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            User targetUser = userService.getUserEntity(userId);
            if (targetUser == null) {
                return ApiResponse.error("用户不存在");
            }
            if (!currentUserId.equals(userId) && targetUser.getHideLikes() != null && targetUser.getHideLikes() == 1) {
                return ApiResponse.error("该用户已隐藏点赞列表");
            }

            List<PostResponse> posts = postService.getLikedPostsByUserWithInfo(userId, currentUserId);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 更新用户资料（包含兴趣标签） =====
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            String nickname = (String) params.get("nickname");
            String avatar = (String) params.get("avatar");
            String email = (String) params.get("email");
            String bio = (String) params.get("bio");
            String interestTags = (String) params.get("interestTags");  // ✅ 新增
            
            Integer hideLikes = (Integer) params.get("hideLikes");
            Integer hideComments = (Integer) params.get("hideComments");
            Integer hideFriends = (Integer) params.get("hideFriends");
            Integer hidePosts = (Integer) params.get("hidePosts");

            UserResponse updatedUser = userService.updateProfile(
                userId, nickname, avatar, email, bio,
                hideLikes, hideComments, hideFriends, hidePosts,
                interestTags  // ✅ 新增
            );
            
            return ApiResponse.success(updatedUser);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取当前用户点赞过的所有动态 =====
    @GetMapping("/likes")
    public ApiResponse<List<Post>> getLikedPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<Post> posts = postService.getLikedPostsByUser(userId);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取用户评论过的所有动态 =====
    @GetMapping("/comments/posts")
    public ApiResponse<List<Post>> getCommentedPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<Post> posts = postService.getCommentedPostsByUser(userId);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取用户的所有评论 =====
    @GetMapping("/comments")
    public ApiResponse<List<Comment>> getMyComments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<Comment> comments = postService.getCommentsByUser(userId);
            return ApiResponse.success(comments);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 更新用户兴趣标签 =====
    @PutMapping("/interests")
    public ApiResponse<UserResponse> updateInterests(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            String interestTags = params.get("interestTags");
            UserResponse updatedUser = userService.updateInterests(userId, interestTags);
            return ApiResponse.success(updatedUser);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}