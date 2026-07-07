package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.UserResponse;
import com.social.backend.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 获取当前用户资料
    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile(HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }
        
        try {
            UserResponse user = userService.getCurrentUser(currentUser.getId());
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 更新用户资料
    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(@RequestBody Map<String, String> params, HttpSession session) {
        UserResponse currentUser = (UserResponse) session.getAttribute("user");
        if (currentUser == null) {
            return ApiResponse.error("请先登录");
        }
        
        try {
            String nickname = params.get("nickname");
            String avatar = params.get("avatar");
            String email = params.get("email");
            
            UserResponse updatedUser = userService.updateProfile(
                currentUser.getId(), 
                nickname, 
                avatar, 
                email
            );
            
            // 更新Session中的用户信息
            session.setAttribute("user", updatedUser);
            
            return ApiResponse.success(updatedUser);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}