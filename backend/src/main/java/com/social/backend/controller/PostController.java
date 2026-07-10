package com.social.backend.controller;

import com.social.backend.dto.*;
import com.social.backend.entity.Comment;
import com.social.backend.entity.Post;
import com.social.backend.entity.User;
import com.social.backend.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    // 发布动态
    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody PostRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            PostResponse post = postService.createPost(userId, request);
            return ApiResponse.success(post);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取动态列表
    @GetMapping
    public ApiResponse<List<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            List<PostResponse> posts = postService.getPosts(userId, page, size);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取单条动态详情
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            PostResponse post = postService.getPostById(id, userId);
            return ApiResponse.success(post);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 删除动态
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            postService.deletePost(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 删除评论
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            postService.deleteComment(commentId, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 点赞/取消点赞
    @PostMapping("/{id}/like")
    public ApiResponse<Integer> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            int likesCount = postService.toggleLike(id, userId);
            return ApiResponse.success(likesCount);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 发表评论（支持回复）
    @PostMapping("/{id}/comments")
    public ApiResponse<Comment> addComment(
            @PathVariable Long id,
            @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            Comment comment = postService.addComment(id, userId, request);
            return ApiResponse.success(comment);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取评论列表（树形结构，支持回复）
    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentResponse>> getComments(@PathVariable Long id) {
        try {
            List<CommentResponse> comments = postService.getCommentsWithReplies(id);
            return ApiResponse.success(comments);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取点赞用户列表
    @GetMapping("/{id}/likes")
    public ApiResponse<List<User>> getLikedUsers(@PathVariable Long id) {
        try {
            List<User> users = postService.getLikedUsers(id);
            return ApiResponse.success(users);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取当前用户点赞过的所有动态
    @GetMapping("/likes")
    public ApiResponse<List<Post>> getLikedPosts(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
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

    // ===== ✅ 新增：获取个性化推荐动态 =====
    @GetMapping("/recommend")
    public ApiResponse<List<PostResponse>> getRecommendPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<PostResponse> posts = postService.getRecommendPosts(userId, page, size);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}