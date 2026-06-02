package com.collab.service.impl;

import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.mapper.UserRoleMapper;
import com.collab.service.LogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoServiceImpl implements LogoService {

    private final UserRoleMapper userRoleMapper;

    // 从配置文件中读取文件上传根目录
    @Value("${file.upload-path}")
    private String uploadPath;

    // 固定Logo子目录和文件名
    private static final String LOGO_DIR = "logo";
    private static final String LOGO_FILENAME = "system-logo.png";

    @Override
    public String getLogoUrl() {
        // 返回静态资源访问路径，注意对应WebMvcConfig中配置的映射
        // 例如：/uploads/logo/system-logo.png
        return "/uploads/logo/" + LOGO_FILENAME;
    }

    @Override
    public String uploadLogo(MultipartFile file) {
        // 权限校验：只有超级管理员可以上传
        Long currentUserId = LoginUserContext.getUserId();
        boolean isAdmin = userRoleMapper.existsAdminRole(currentUserId);
        if (!isAdmin) {
            throw new BusinessException("只有超级管理员才能修改系统Logo");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }

        // 构建保存路径：uploadPath/logo/
        Path logoDir = Paths.get(uploadPath, LOGO_DIR);
        try {
            // 创建目录（如果不存在）
            if (!Files.exists(logoDir)) {
                Files.createDirectories(logoDir);
            }
            // 目标文件路径
            Path targetPath = logoDir.resolve(LOGO_FILENAME);
            // 保存文件（覆盖）
            file.transferTo(targetPath.toFile());
            log.info("Logo已更新，保存路径：{}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("保存Logo文件失败", e);
            throw new BusinessException("上传Logo失败：" + e.getMessage());
        }

        // 返回新的访问URL
        return getLogoUrl();
    }
}