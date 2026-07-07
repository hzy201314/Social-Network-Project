package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.LoginRequest;
import com.social.backend.dto.RegisterRequest;
import com.social.backend.dto.UserResponse;
import com.social.backend.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse user = userService.register(request.getUsername(), request.getPassword());
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        try {
            UserResponse user = userService.login(request.getUsername(), request.getPassword());
            session.setAttribute("user", user);
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.removeAttribute("user");
        return ApiResponse.success();
    }
}