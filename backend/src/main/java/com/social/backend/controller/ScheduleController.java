package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import com.social.backend.dto.ScheduleRequest;
import com.social.backend.dto.ScheduleResponse;
import com.social.backend.dto.ScheduleStatusRequest;
import com.social.backend.service.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // ===== 创建日程 =====
    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(@RequestBody ScheduleRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            ScheduleResponse schedule = scheduleService.createSchedule(userId, request);
            return ApiResponse.success(schedule);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 获取用户所有日程 =====
    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getUserSchedules(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            List<ScheduleResponse> schedules = scheduleService.getUserSchedules(userId);
            return ApiResponse.success(schedules);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== ✅ 修改：获取用户某天的日程（用 String 接收日期） =====
    @GetMapping("/date")
    public ApiResponse<List<ScheduleResponse>> getUserSchedulesByDate(
            @RequestParam String date,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            // 手动解析日期：yyyy-MM-dd -> yyyy-MM-ddT00:00:00
            LocalDateTime dateTime = LocalDateTime.parse(date + "T00:00:00");
            List<ScheduleResponse> schedules = scheduleService.getUserSchedulesByDate(userId, dateTime);
            return ApiResponse.success(schedules);
        } catch (Exception e) {
            return ApiResponse.error("日期格式错误，请使用 yyyy-MM-dd 格式，例如：2026-07-10");
        }
    }

    // ===== 获取用户某时间范围内的日程 =====
    @GetMapping("/range")
    public ApiResponse<List<ScheduleResponse>> getUserSchedulesByRange(
            @RequestParam String start,
            @RequestParam String end,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            LocalDateTime startTime = LocalDateTime.parse(start);
            LocalDateTime endTime = LocalDateTime.parse(end);
            List<ScheduleResponse> schedules = scheduleService.getUserSchedulesByRange(userId, startTime, endTime);
            return ApiResponse.success(schedules);
        } catch (Exception e) {
            return ApiResponse.error("日期格式错误，请使用 yyyy-MM-ddTHH:mm:ss 格式");
        }
    }

    // ===== 获取单条日程详情 =====
    @GetMapping("/{id}")
    public ApiResponse<ScheduleResponse> getScheduleById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            ScheduleResponse schedule = scheduleService.getScheduleById(id, userId);
            return ApiResponse.success(schedule);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 更新日程 =====
    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @RequestBody ScheduleRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            ScheduleResponse schedule = scheduleService.updateSchedule(id, userId, request);
            return ApiResponse.success(schedule);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 更新日程状态 =====
    @PutMapping("/{id}/status")
    public ApiResponse<ScheduleResponse> updateScheduleStatus(
            @PathVariable Long id,
            @RequestBody ScheduleStatusRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            ScheduleResponse schedule = scheduleService.updateScheduleStatus(id, userId, request.getStatus());
            return ApiResponse.success(schedule);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ===== 删除日程 =====
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("请先登录");
        }

        try {
            scheduleService.deleteSchedule(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}