package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.CommentRequest;
import com.social.backend.dto.PostRequest;
import com.social.backend.dto.PostResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.entity.Comment;
import com.social.backend.service.PostService;
import jakarta.servlet.http.HttpSession;
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
    public ApiResponse<PostResponse> createPost(@RequestBody PostRequest request, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            PostResponse post = postService.createPost(currentUser.getId(), request);
            return ApiResponse.success(post);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取动态列表（分页）
    @GetMapping
    public ApiResponse<List<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        Long userId = currentUser != null ? currentUser.getId() : null;

        try {
            List<PostResponse> posts = postService.getPosts(userId, page, size);
            return ApiResponse.success(posts);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取单条动态详情
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        Long userId = currentUser != null ? currentUser.getId() : null;

        try {
            PostResponse post = postService.getPostById(id, userId);
            return ApiResponse.success(post);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 删除动态
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            postService.deletePost(id, currentUser.getId());
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 点赞/取消点赞
    @PostMapping("/{id}/like")
    public ApiResponse<Integer> toggleLike(@PathVariable Long id, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            int likesCount = postService.toggleLike(id, currentUser.getId());
            return ApiResponse.success(likesCount);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 发表评论
    @PostMapping("/{id}/comments")
    public ApiResponse<Comment> addComment(@PathVariable Long id, @RequestBody CommentRequest request, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            Comment comment = postService.addComment(id, currentUser.getId(), request);
            return ApiResponse.success(comment);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 获取动态的评论列表
    @GetMapping("/{id}/comments")
    public ApiResponse<List<Comment>> getComments(@PathVariable Long id) {
        try {
            List<Comment> comments = postService.getComments(id);
            return ApiResponse.success(comments);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}