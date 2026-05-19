package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<Object> upload(
            MultipartFile file,
            Long taskId,
            Long projectId
    ){

        return Result.success(
                fileService.upload(
                        file,
                        taskId,
                        projectId
                )
        );
    }

    /**
     * 文件列表
     */
    @GetMapping("/list")
    public Result<Object> list(
            @RequestParam(required = false)
            Long taskId,

            @RequestParam(required = false)
            Long projectId
    ){

        return Result.success(
                fileService.listFile(
                        taskId,
                        projectId
                )
        );
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ){

        fileService.deleteFile(id);

        return Result.success();
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(
            @PathVariable Long id
    ){

        return fileService.download(id);
    }

}