package com.collab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.entity.FileInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileService
        extends IService<FileInfo> {

    Map<String,Object> upload(
            MultipartFile file,
            Long taskId,
            Long projectId
    );

    List<Object> listFile(
            Long taskId,
            Long projectId
    );

    void deleteFile(Long id);

    ResponseEntity<Resource> download(Long id);

}