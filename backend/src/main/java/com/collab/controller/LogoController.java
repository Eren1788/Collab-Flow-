package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.service.LogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/logo")
@RequiredArgsConstructor
@Tag(name = "Logo管理", description = "系统Logo上传与获取")
public class LogoController {

    private final LogoService logoService;

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 获取Logo URL（公开接口，无需登录）
     */
    @GetMapping("/url")
    @Operation(summary = "获取Logo URL")
    public Result<String> getLogoUrl() {
        return Result.success(logoService.getLogoUrl());
    }

    /**
     * 新增：直接返回Logo图片字节流（无需静态资源映射）
     */
    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getLogoImage() {
        Path imagePath = Paths.get(uploadPath, "logo", "system-logo.png");
        if (!Files.exists(imagePath)) {
            log.warn("Logo图片不存在: {}", imagePath.toAbsolutePath());
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String contentType = Files.probeContentType(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/png"))
                    .body(imageBytes);
        } catch (IOException e) {
            log.error("读取Logo图片失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 上传/更新Logo（仅超级管理员）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传Logo")
    public Result<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String url = logoService.uploadLogo(file);
        return Result.success(Map.of("url", url));
    }
}