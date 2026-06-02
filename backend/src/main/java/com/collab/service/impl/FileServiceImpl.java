package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.exception.BusinessException;
import com.collab.common.utils.LoginUserContext;
import com.collab.entity.FileInfo;
import com.collab.entity.Project;
import com.collab.entity.ProjectMember;
import com.collab.entity.User;
import com.collab.mapper.FileInfoMapper;
import com.collab.mapper.ProjectMapper;
import com.collab.mapper.ProjectMemberMapper;
import com.collab.mapper.UserMapper;
import com.collab.mapper.UserRoleMapper;
import com.collab.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileService {

    private final FileInfoMapper fileInfoMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserRoleMapper userRoleMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public List<Map<String, Object>> getAccessibleProjects() {
        Long currentUserId = LoginUserContext.getUserId();
        Long roleId = userRoleMapper.getRoleIdByUserId(currentUserId);
        boolean isAdmin = (roleId != null && roleId == 1L);

        List<Project> projects;
        if (isAdmin) {
            // 管理员看到所有项目
            projects = projectMapper.selectList(null);
        } else {
            // 非管理员只能看到自己参与的项目
            LambdaQueryWrapper<ProjectMember> pmWrapper = new LambdaQueryWrapper<>();
            pmWrapper.eq(ProjectMember::getUserId, currentUserId);
            List<ProjectMember> members = projectMemberMapper.selectList(pmWrapper);
            if (members.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> projectIds = members.stream()
                    .map(ProjectMember::getProjectId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<Project> pWrapper = new LambdaQueryWrapper<>();
            pWrapper.in(Project::getId, projectIds);
            projects = projectMapper.selectList(pWrapper);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Project p : projects) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            result.add(map);
        }
        // 按ID降序
        result.sort((a, b) -> Long.compare(
                ((Number) b.get("id")).longValue(),
                ((Number) a.get("id")).longValue()
        ));
        return result;
    }

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

        Long currentUserId = LoginUserContext.getUserId();

        // 1. 确定当前用户能看到哪些项目的文件
        // 超级管理员(roleId=1)可以看到所有，其他用户只能看自己参与的项目
        Long roleId = userRoleMapper.getRoleIdByUserId(currentUserId);
        boolean isAdmin = (roleId != null && roleId == 1L);

        // 限定到当前用户有权限的项目ID列表
        List<Long> allowedProjectIds = null; // null = 不限
        if (!isAdmin) {
            // 查询用户参与的项目
            LambdaQueryWrapper<ProjectMember> pmWrapper = new LambdaQueryWrapper<>();
            pmWrapper.eq(ProjectMember::getUserId, currentUserId);
            List<ProjectMember> members = projectMemberMapper.selectList(pmWrapper);
            allowedProjectIds = members.stream()
                    .map(ProjectMember::getProjectId)
                    .collect(Collectors.toList());
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();

        // 按 taskId 查询：用户已能访问该任务，跳过项目权限过滤
        if (taskId != null) {
            wrapper.eq(FileInfo::getTaskId, taskId);
        } else {
            // 按项目查询时才做权限过滤
            if (projectId != null) {
                // 非管理员不能查非自己参与的项目
                if (!isAdmin && (allowedProjectIds == null || !allowedProjectIds.contains(projectId))) {
                    return Collections.emptyList();
                }
                wrapper.eq(FileInfo::getProjectId, projectId);
            } else {
                // 没有指定 projectId，则按权限范围过滤
                if (allowedProjectIds != null && !allowedProjectIds.isEmpty()) {
                    wrapper.in(FileInfo::getProjectId, allowedProjectIds);
                } else if (allowedProjectIds != null && allowedProjectIds.isEmpty()) {
                    return Collections.emptyList();
                }
            }
        }

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

            map.put("taskId", file.getTaskId());

            map.put("projectId", file.getProjectId());

            // 上传人姓名
            String uploaderName = "";
            if (file.getUploaderId() != null) {
                User uploader = userMapper.selectById(file.getUploaderId());
                if (uploader != null) {
                    uploaderName = uploader.getNickname() != null
                            ? uploader.getNickname()
                            : uploader.getUsername();
                }
            }
            map.put("uploaderName", uploaderName);

            // 项目名称
            String projectName = "";
            if (file.getProjectId() != null) {
                Project proj = projectMapper.selectById(file.getProjectId());
                if (proj != null) {
                    projectName = proj.getName();
                }
            }
            map.put("projectName", projectName);

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

            String fullPath = uploadPath + File.separator + fileInfo.getUrl();
            log.info("下载文件路径: {}", fullPath);

            File file = new File(fullPath);
            if (!file.exists()) {
                log.error("物理文件不存在: {}", fullPath);
                throw new BusinessException("物理文件不存在");
            }

            Resource resource = new FileSystemResource(file);

            String fileName = URLEncoder.encode(fileInfo.getName(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(fileName, StandardCharsets.UTF_8)
                                    .build()
                                    .toString())
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }
}