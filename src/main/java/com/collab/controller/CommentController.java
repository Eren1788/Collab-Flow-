package com.collab.controller;

import com.collab.common.result.Result;
import com.collab.dto.CommentDTO;
import com.collab.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/add")
    public Result<Void> add(@RequestBody CommentDTO dto) {
        try {
            commentService.addComment(dto);
        } catch (Exception e) {
            throw new RuntimeException("评论发送失败: " + e.getMessage(), e);
        }
        return Result.success();
    }

    @GetMapping("/list/{taskId}")
    public Result<Object> list(@PathVariable Long taskId) {
        return Result.success(commentService.listComment(taskId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }


    @PostMapping("/project/add")
    public Result<Void> addProjectComment(@RequestBody CommentDTO dto) {
        try {
            commentService.addProjectComment(dto);
        } catch (Exception e) {
            throw new RuntimeException("项目评论发送失败: " + e.getMessage(), e);
        }
        return Result.success();
    }

    @GetMapping("/project/list/{projectId}")
    public Result<Object> listProjectComment(@PathVariable Long projectId) {
        return Result.success(commentService.listProjectComment(projectId));
    }
}