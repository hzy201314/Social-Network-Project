package com.social.backend.service;

import com.social.backend.dto.UserResponse;
import com.social.backend.entity.User;
import com.social.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 注册
    public UserResponse register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setNickname(savedUser.getNickname());
        response.setAvatar(savedUser.getAvatar());
        response.setEmail(savedUser.getEmail());
        response.setBio(savedUser.getBio());
        // 隐私字段默认值
        response.setHideLikes(0);
        response.setHideComments(0);
        response.setHideFriends(0);
        response.setHidePosts(0);
        return response;
    }

    // 登录
    public UserResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        response.setBio(user.getBio());
        // 返回隐私字段
        response.setHideLikes(user.getHideLikes());
        response.setHideComments(user.getHideComments());
        response.setHideFriends(user.getHideFriends());
        response.setHidePosts(user.getHidePosts());
        return response;
    }

    // 获取当前用户信息
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        response.setBio(user.getBio());
        // 返回隐私字段
        response.setHideLikes(user.getHideLikes());
        response.setHideComments(user.getHideComments());
        response.setHideFriends(user.getHideFriends());
        response.setHidePosts(user.getHidePosts());
        return response;
    }

    // 更新用户资料（包含隐私设置）
    public UserResponse updateProfile(
            Long userId, 
            String nickname, 
            String avatar, 
            String email, 
            String bio,
            Integer hideLikes, 
            Integer hideComments, 
            Integer hideFriends, 
            Integer hidePosts) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar);
        }
        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        // 更新隐私设置
        if (hideLikes != null) {
            user.setHideLikes(hideLikes);
        }
        if (hideComments != null) {
            user.setHideComments(hideComments);
        }
        if (hideFriends != null) {
            user.setHideFriends(hideFriends);
        }
        if (hidePosts != null) {
            user.setHidePosts(hidePosts);
        }
        
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        UserResponse response = new UserResponse();
        response.setId(updatedUser.getId());
        response.setUsername(updatedUser.getUsername());
        response.setNickname(updatedUser.getNickname());
        response.setAvatar(updatedUser.getAvatar());
        response.setEmail(updatedUser.getEmail());
        response.setBio(updatedUser.getBio());
        response.setHideLikes(updatedUser.getHideLikes());
        response.setHideComments(updatedUser.getHideComments());
        response.setHideFriends(updatedUser.getHideFriends());
        response.setHidePosts(updatedUser.getHidePosts());
        return response;
    }
}