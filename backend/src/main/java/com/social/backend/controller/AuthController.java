package com.social.backend.controller;

import com.social.backend.dto.*;
import com.social.backend.service.UserService;
import com.social.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse user = userService.register(request.getUsername(), request.getPassword());
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 修改登录接口 =====
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            // 登录验证
            UserResponse user = userService.login(request.getUsername(), request.getPassword());
            
            // 生成 JWT Token
            String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getNickname()
            );
            
            // 返回 Token 和用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            
            return ApiResponse.success(data);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT 无状态，前端删除 Token 即可
        return ApiResponse.success();
    }
}