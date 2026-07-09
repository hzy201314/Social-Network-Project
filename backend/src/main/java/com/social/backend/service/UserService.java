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
        response.setBio(savedUser.getBio());   // ✅ 新增
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
        response.setBio(user.getBio());   // ✅ 新增
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
        response.setBio(user.getBio());   // ✅ 新增
        return response;
    }

    // 更新用户资料
    public UserResponse updateProfile(Long userId, String nickname, String avatar, String email, String bio) {
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
        if (bio != null && !bio.isEmpty()) {
            user.setBio(bio);   // ✅ 新增
        }
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        UserResponse response = new UserResponse();
        response.setId(updatedUser.getId());
        response.setUsername(updatedUser.getUsername());
        response.setNickname(updatedUser.getNickname());
        response.setAvatar(updatedUser.getAvatar());
        response.setEmail(updatedUser.getEmail());
        response.setBio(updatedUser.getBio());   // ✅ 新增
        return response;
    }
}