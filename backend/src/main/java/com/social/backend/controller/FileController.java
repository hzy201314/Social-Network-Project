package com.social.backend.controller;

import com.social.backend.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.access.url}")
    private String accessUrl;

    // ===== 单文件上传 =====
    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.error("文件不能为空");
            }

            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + suffix;

            // 创建目录（如果不存在）
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存文件
            File saveFile = new File(uploadPath + newFilename);
            file.transferTo(saveFile);

            // 返回访问 URL
            String fileUrl = accessUrl + newFilename;
            System.out.println("✅ 文件上传成功: " + fileUrl);
            return ApiResponse.success(fileUrl);

        } catch (IOException e) {
            return ApiResponse.error("文件上传失败：" + e.getMessage());
        }
    }

    // ===== 多文件上传 =====
    @PostMapping("/upload/multiple")
    public ApiResponse<List<String>> uploadMultipleFiles(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                String suffix = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String newFilename = UUID.randomUUID().toString() + suffix;

                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                File saveFile = new File(uploadPath + newFilename);
                file.transferTo(saveFile);

                String fileUrl = accessUrl + newFilename;
                urls.add(fileUrl);
                System.out.println("✅ 文件上传成功: " + fileUrl);
            }

            return ApiResponse.success(urls);
        } catch (IOException e) {
            return ApiResponse.error("文件上传失败：" + e.getMessage());
        }
    }
}