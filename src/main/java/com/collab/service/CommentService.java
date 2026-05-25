package com.collab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.dto.CommentDTO;
import com.collab.entity.Comment;
import com.collab.vo.CommentVO;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.util.List;

public interface CommentService
        extends IService<Comment> {

    void addComment(CommentDTO dto) throws JsonProcessingException;

    List<CommentVO> listComment(Long taskId);

    void deleteComment(Long id);

    //项目评论
    void addProjectComment(CommentDTO dto) throws JsonProcessingException;

    List<CommentVO> listProjectComment(Long projectId);

}