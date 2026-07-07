package com.social.backend.service;

import com.social.backend.dto.CommentRequest;
import com.social.backend.dto.PostRequest;
import com.social.backend.dto.PostResponse;
import com.social.backend.entity.Comment;
import com.social.backend.entity.Like;
import com.social.backend.entity.Post;
import com.social.backend.entity.User;
import com.social.backend.repository.CommentRepository;
import com.social.backend.repository.LikeRepository;
import com.social.backend.repository.PostRepository;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    // 发布动态
    public PostResponse createPost(Long userId, PostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            post.setImages(String.join(",", request.getImages()));
        }
        post.setLikesCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return convertToResponse(savedPost, user);
    }

    // 获取动态列表（分页）
    public List<PostResponse> getPosts(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findAll(pageable);
        
        return postPage.getContent().stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId())
                            .orElse(null);
                    return convertToResponse(post, user, currentUserId);
                })
                .collect(Collectors.toList());
    }

    // 获取单条动态详情
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return convertToResponse(post, user, currentUserId);
    }

    // 删除动态
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此动态");
        }
        
        // 删除点赞记录
        likeRepository.deleteAll(likeRepository.findAll().stream()
                .filter(like -> like.getPostId().equals(postId))
                .collect(Collectors.toList()));
        
        // 删除评论
        commentRepository.deleteAll(commentRepository.findByPostIdOrderByCreatedAtAsc(postId));
        
        // 删除动态
        postRepository.delete(post);
    }

    // 点赞/取消点赞
    @Transactional
    public int toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        java.util.Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            // 取消点赞
            likeRepository.delete(existingLike.get());
            post.setLikesCount(post.getLikesCount() - 1);
            postRepository.save(post);
            return post.getLikesCount();
        } else {
            // 点赞
            Like like = new Like();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
            return post.getLikesCount();
        }
    }

    // 发表评论
    public Comment addComment(Long postId, Long userId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    // 获取动态的评论列表
    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    // 转换方法：带当前用户（用于判断是否点赞）
    private PostResponse convertToResponse(Post post, User user, Long currentUserId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setUsername(user != null ? user.getUsername() : "未知用户");
        response.setNickname(user != null ? user.getNickname() : "未知用户");
        response.setAvatar(user != null ? user.getAvatar() : null);
        response.setContent(post.getContent());
        
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            response.setImages(Arrays.asList(post.getImages().split(",")));
        } else {
            response.setImages(new ArrayList<>());
        }
        
        response.setLikesCount(post.getLikesCount());
        response.setCommentCount(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).size());
        
        // 判断当前用户是否点赞
        if (currentUserId != null) {
            response.setLiked(likeRepository.findByPostIdAndUserId(post.getId(), currentUserId).isPresent());
        } else {
            response.setLiked(false);
        }
        
        response.setCreatedAt(post.getCreatedAt());
        return response;
    }

    // 转换方法：不带当前用户（用于创建时）
    private PostResponse convertToResponse(Post post, User user) {
        return convertToResponse(post, user, null);
    }
}