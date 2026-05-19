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

    /**
     * 新增评论
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody CommentDTO dto
    ){

        commentService.addComment(dto);

        return Result.success();
    }

    /**
     * 评论列表
     */
    @GetMapping("/list/{taskId}")
    public Result<Object> list(
            @PathVariable Long taskId
    ){

        return Result.success(
                commentService.listComment(taskId)
        );
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(
            @PathVariable Long id
    ){

        commentService.deleteComment(id);

        return Result.success();
    }

}