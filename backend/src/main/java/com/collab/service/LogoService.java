package com.collab.service;

import org.springframework.web.multipart.MultipartFile;

public interface LogoService {
    /**
     * 获取系统Logo的访问URL
     */
    String getLogoUrl();

    /**
     * 上传Logo（覆盖旧文件）
     * @param file 图片文件
     * @return 新的访问URL
     */
    String uploadLogo(MultipartFile file);
}