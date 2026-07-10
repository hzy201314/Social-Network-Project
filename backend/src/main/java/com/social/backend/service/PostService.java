package com.social.backend.service;

import com.social.backend.dto.CommentRequest;
import com.social.backend.dto.CommentResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FriendService friendService;  // ✅ 新增

    // 发布动态
    public PostResponse createPost(Long userId, PostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Post post = new Post();
        post.setUserId(userId);

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            String content = request.getContent();
            if (content != null && !content.isEmpty()) {
                title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
            } else {
                title = "分享了一条动态";
            }
        }
        post.setTitle(title);

        post.setContent(request.getContent());
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            post.setImages(String.join(",", request.getImages()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            post.setTags(String.join(",", request.getTags()));
        }
        post.setLikesCount(0);
        post.setIsDeleted(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return convertToResponse(savedPost, user);
    }

    // ===== 获取动态列表（分页，只查未删除的） =====
    public List<PostResponse> getPosts(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> postPage = postRepository.findByIsDeleted(0, pageable);
        
        return postPage.getContent().stream()
                .map(post -> {
                    User user = userRepository.findById(post.getUserId())
                            .orElse(null);
                    return convertToResponse(post, user, currentUserId);
                })
                .collect(Collectors.toList());
    }

    // ===== 获取指定用户的动态列表 =====
    public List<Post> getPostsByUserId(Long userId) {
        return postRepository.findByUserIdAndIsDeleted(userId, 0);
    }

    // ===== 获取指定用户的动态列表（带用户信息） =====
    public List<PostResponse> getPostsByUserIdWithUserInfo(Long userId, Long currentUserId) {
        List<Post> posts = postRepository.findByUserIdAndIsDeleted(userId, 0);
        User user = userRepository.findById(userId).orElse(null);
        
        final Long finalCurrentUserId = currentUserId;
        return posts.stream()
                .map(post -> convertToResponse(post, user, finalCurrentUserId))
                .collect(Collectors.toList());
    }

    // 获取单条动态详情
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (post.getIsDeleted() != null && post.getIsDeleted() == 1) {
            User user = userRepository.findById(post.getUserId()).orElse(null);
            return convertToResponse(post, user, currentUserId);
        }
        
        User user = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return convertToResponse(post, user, currentUserId);
    }

    // ===== 删除动态（软删除） =====
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此动态");
        }
        
        post.setIsDeleted(1);
        post.setContent(null);
        post.setImages(null);
        postRepository.save(post);
    }

    // 点赞/取消点赞
    @Transactional
    public int toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        java.util.Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            post.setLikesCount(post.getLikesCount() - 1);
            postRepository.save(post);
            return post.getLikesCount();
        } else {
            Like like = new Like();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);

            if (!post.getUserId().equals(userId)) {
                User sender = userRepository.findById(userId).orElse(null);
                String content = (sender != null ? sender.getNickname() : "用户") + " 赞了你的动态";
                notificationService.sendNotification(
                    post.getUserId(),
                    userId,
                    "like",
                    postId,
                    content
                );
            }
            return post.getLikesCount();
        }
    }

    // 发表评论（支持回复）
    @Transactional
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

        Long replyToUserId = null;

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            comment.setParentId(request.getParentId());
            replyToUserId = parent.getUserId();
        }

        Comment savedComment = commentRepository.save(comment);

        if (!post.getUserId().equals(userId)) {
            String content = (user != null ? user.getNickname() : "用户") + " 评论了你的动态: " + request.getContent();
            notificationService.sendNotification(
                post.getUserId(),
                userId,
                "comment",
                postId,
                content
            );
        }

        if (replyToUserId != null && !replyToUserId.equals(userId) && !replyToUserId.equals(post.getUserId())) {
            String content = (user != null ? user.getNickname() : "用户") + " 回复了你的评论: " + request.getContent();
            notificationService.sendNotification(
                replyToUserId,
                userId,
                "comment",
                postId,
                content
            );
        }

        return savedComment;
    }

    // 获取评论列表（树形结构）
    public List<CommentResponse> getCommentsWithReplies(Long postId) {
        List<Comment> topComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId);
        
        return topComments.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
    }

    // 获取点赞用户列表
    public List<User> getLikedUsers(Long postId) {
        List<Like> likes = likeRepository.findByPostId(postId);
        return likes.stream()
                .map(like -> userRepository.findById(like.getUserId()).orElse(null))
                .filter(user -> user != null)
                .collect(Collectors.toList());
    }

    // 获取用户点赞过的所有动态
    public List<Post> getLikedPostsByUser(Long userId) {
        List<Like> likes = likeRepository.findByUserId(userId);
        return likes.stream()
                .map(like -> postRepository.findById(like.getPostId()).orElse(null))
                .filter(post -> post != null && post.getIsDeleted() == 0)
                .collect(Collectors.toList());
    }

    // ===== 获取用户点赞过的所有动态（带用户信息） =====
    public List<PostResponse> getLikedPostsByUserWithInfo(Long userId, Long currentUserId) {
        List<Like> likes = likeRepository.findByUserId(userId);
        return likes.stream()
                .map(like -> {
                    Post post = postRepository.findById(like.getPostId()).orElse(null);
                    if (post == null || post.getIsDeleted() == 1) {
                        return null;
                    }
                    User postUser = userRepository.findById(post.getUserId()).orElse(null);
                    return convertToResponse(post, postUser, currentUserId);
                })
                .filter(post -> post != null)
                .collect(Collectors.toList());
    }

    // 获取用户评论过的所有动态
    public List<Post> getCommentedPostsByUser(Long userId) {
        List<Long> postIds = commentRepository.findDistinctPostIdsByUserId(userId);
        return postIds.stream()
                .map(postId -> postRepository.findById(postId).orElse(null))
                .filter(post -> post != null && post.getIsDeleted() == 0)
                .collect(Collectors.toList());
    }

    // 获取用户的所有评论
    public List<Comment> getCommentsByUser(Long userId) {
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 删除评论（软删除）
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        if (!comment.getUserId().equals(userId) && !post.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此评论");
        }

        comment.setIsDeleted(1);
        comment.setContent(null);
        commentRepository.save(comment);
    }

    // ===== ✅ 新增：获取个性化推荐动态 =====
    public List<PostResponse> getRecommendPosts(Long userId, int page, int size) {
        User user = userRepository.findById(userId).orElse(null);
        List<String> userInterests = new ArrayList<>();
        
        // 1. 获取用户兴趣标签
        if (user != null && user.getInterestTags() != null && !user.getInterestTags().isEmpty()) {
            userInterests = Arrays.asList(user.getInterestTags().split(","));
            userInterests = userInterests.stream().map(String::trim).collect(Collectors.toList());
        }
        
        // 2. 获取好友ID列表
        List<Long> friendIds = friendService.getFriendIds(userId);
        
        // 3. 获取所有未删除的动态
        List<Post> allPosts = postRepository.findByIsDeleted(0, Pageable.unpaged()).getContent();
        
        // 4. 计算每条动态的分数
        List<PostScore> postScores = new ArrayList<>();
        for (Post post : allPosts) {
            if (post.getUserId().equals(userId)) continue;
            
            int score = 0;
            List<String> matchedTags = new ArrayList<>();
            
            if (friendIds.contains(post.getUserId())) {
                score += 10;
            }
            
            if (post.getTags() != null && !post.getTags().isEmpty()) {
                List<String> postTags = Arrays.asList(post.getTags().split(","));
                postTags = postTags.stream().map(String::trim).collect(Collectors.toList());
                for (String tag : postTags) {
                    if (userInterests.contains(tag)) {
                        score += 5;
                        matchedTags.add(tag);
                    }
                }
            }
            
            int likeScore = (post.getLikesCount() != null ? post.getLikesCount() : 0) / 10;
            score += likeScore;
            
            int commentCount = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(post.getId()).size();
            score += commentCount / 5;
            
            if (post.getCreatedAt() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    post.getCreatedAt(), 
                    LocalDateTime.now()
                );
                if (days < 1) score += 5;
                else if (days < 3) score += 3;
                else if (days < 7) score += 1;
            }
            
            score = Math.max(score, 1);
            postScores.add(new PostScore(post, score, matchedTags));
        }
        
        // 5. 按分数降序排序
        postScores.sort((a, b) -> b.score - a.score);
        
        // 6. 分页
        int start = page * size;
        int end = Math.min(start + size, postScores.size());
        if (start >= postScores.size()) {
            return new ArrayList<>();
        }
        List<PostScore> pagedScores = postScores.subList(start, end);
        
        // 7. 转换为 PostResponse
        return pagedScores.stream()
                .map(ps -> {
                    Post post = ps.post;
                    User postUser = userRepository.findById(post.getUserId()).orElse(null);
                    return convertToResponse(post, postUser, userId);
                })
                .collect(Collectors.toList());
    }

    // ===== 内部类：动态分数 =====
    private static class PostScore {
        Post post;
        int score;
        List<String> matchedTags;
        
        PostScore(Post post, int score, List<String> matchedTags) {
            this.post = post;
            this.score = score;
            this.matchedTags = matchedTags;
        }
    }

    // ========== 转换方法 ==========

    // 转换评论（递归加载子评论）
    private CommentResponse convertToCommentResponse(Comment comment) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);
        
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setUserId(comment.getUserId());
        response.setParentId(comment.getParentId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setIsDeleted(comment.getIsDeleted());
        
        if (comment.getIsDeleted() != null && comment.getIsDeleted() == 1) {
            response.setContent("该评论已删除");
            response.setUsername("用户已注销");
            response.setNickname("已注销");
            response.setAvatar(null);
        } else {
            response.setContent(comment.getContent());
            if (user != null) {
                response.setUsername(user.getUsername());
                response.setNickname(user.getNickname());
                response.setAvatar(user.getAvatar());
            }
        }
        
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::convertToCommentResponse)
                .collect(Collectors.toList());
        response.setReplies(replyResponses);
        
        return response;
    }

    // ===== 转换动态 =====
    private PostResponse convertToResponse(Post post, User user, Long currentUserId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setUserId(post.getUserId());
        response.setIsDeleted(post.getIsDeleted());
        response.setTitle(post.getTitle());

        if (post.getTags() != null && !post.getTags().isEmpty()) {
            response.setTags(Arrays.asList(post.getTags().split(",")));
        } else {
            response.setTags(new ArrayList<>());
        }
        
        if (post.getIsDeleted() != null && post.getIsDeleted() == 1) {
            response.setContent("该动态已删除");
            response.setUsername("用户已注销");
            response.setNickname("已注销");
            response.setAvatar(null);
            response.setImages(new ArrayList<>());
            response.setLikesCount(0);
            response.setCommentCount(0);
            response.setLiked(false);
            response.setCreatedAt(post.getCreatedAt());
            return response;
        }
        
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
        response.setCommentCount(commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(post.getId()).size());
        
        if (currentUserId != null) {
            response.setLiked(likeRepository.findByPostIdAndUserId(post.getId(), currentUserId).isPresent());
        } else {
            response.setLiked(false);
        }
        
        response.setCreatedAt(post.getCreatedAt());
        return response;
    }

    // 转换动态（不带当前用户）
    private PostResponse convertToResponse(Post post, User user) {
        return convertToResponse(post, user, null);
    }
}