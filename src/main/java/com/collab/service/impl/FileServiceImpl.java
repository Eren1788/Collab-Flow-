package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.FileInfo;
import com.collab.mapper.FileInfoMapper;
import com.collab.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileService {

    private final FileInfoMapper fileInfoMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public Map<String, Object> upload(
            MultipartFile file,
            Long taskId,
            Long projectId
    ) {

        if (file == null || file.isEmpty()) {

            throw new BusinessException("文件不能为空");
        }

        try {

            // 原文件名
            String originalFilename = file.getOriginalFilename();

            // 后缀名
            String suffix = originalFilename.substring(
                            originalFilename.lastIndexOf(".")
                    );

            // UUID文件名
            String newFileName = UUID.randomUUID() + suffix;

            // 上传目录
            File dir = new File(uploadPath);

            if (!dir.exists()) {

                dir.mkdirs();
            }

            // 最终文件
            File dest = new File(uploadPath
                            + File.separator
                            + newFileName);

            // 保存文件
            file.transferTo(dest);

            // 保存数据库
            FileInfo fileInfo = new FileInfo();

            fileInfo.setTaskId(taskId);

            fileInfo.setProjectId(projectId);

            fileInfo.setName(originalFilename);

            fileInfo.setUrl(newFileName);

            fileInfo.setFileSize(file.getSize());

            fileInfo.setFileType(file.getContentType());

            //fileInfo.setUploaderId(1L);
            fileInfo.setUploaderId(LoginUserContext.getUserId());

            fileInfoMapper.insert(fileInfo);

            Map<String, Object> map = new HashMap<>();

            map.put("id", fileInfo.getId());

            map.put("fileName", originalFilename);

            map.put("url", newFileName);

            return map;

        } catch (Exception e) {

            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public List<Object> listFile(
            Long taskId,
            Long projectId
    ) {

        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(
                taskId != null,
                FileInfo::getTaskId,
                taskId
        );

        wrapper.eq(
                projectId != null,
                FileInfo::getProjectId,
                projectId
        );

        wrapper.orderByDesc(FileInfo::getId);

        List<FileInfo> files = fileInfoMapper.selectList(wrapper);

        List<Object> list = new ArrayList<>();

        for (FileInfo file : files) {
            Map<String, Object> map = new HashMap<>();

            map.put("id", file.getId());

            map.put("name", file.getName());

            map.put("url", file.getUrl());

            map.put("fileSize", file.getFileSize());

            map.put("fileType", file.getFileType());

            map.put("uploadTime", file.getUploadTime());

            list.add(map);
        }

        return list;
    }

    @Override
    public void deleteFile(Long id) {

        FileInfo fileInfo = fileInfoMapper.selectById(id);

        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }

        // 删除物理文件
        File file = new File(uploadPath
                + File.separator
                + fileInfo.getUrl());

        if (file.exists()) {
            file.delete();
        }

        // 删除数据库
        fileInfoMapper.deleteById(id);
    }

    @Override
    public ResponseEntity<Resource> download(Long id) {

        try {

            FileInfo fileInfo = fileInfoMapper.selectById(id);

            if (fileInfo == null) {
                throw new BusinessException("文件不存在");
            }

            File file = new File(uploadPath
                    + File.separator
                    + fileInfo.getUrl());

            Resource resource = new FileSystemResource(file);

            String fileName = URLEncoder.encode(
                    fileInfo.getName(),
                    StandardCharsets.UTF_8
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition
                                    .attachment()
                                    .filename(
                                            fileName,
                                             StandardCharsets.UTF_8
                                    )
                                    .build()
                                    .toString()
                    )
                    .body(resource);

        } catch (Exception e) {

            throw new BusinessException("文件下载失败");
        }
    }
}