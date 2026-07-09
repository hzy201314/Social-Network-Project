package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Comment;
import com.social.backend.entity.Post;
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

    // 获取当前用户资料
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

    // 更新用户资料
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            String nickname = params.get("nickname");
            String avatar = params.get("avatar");
            String email = params.get("email");
            String bio = params.get("bio");   // ✅ 新增

            UserResponse updatedUser = userService.updateProfile(userId, nickname, avatar, email, bio);
            return ApiResponse.success(updatedUser);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取当前用户点赞过的所有动态
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

    // 获取用户评论过的所有动态
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

    // 获取用户的所有评论
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
}