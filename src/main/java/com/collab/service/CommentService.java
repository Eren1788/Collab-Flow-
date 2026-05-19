package com.collab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.dto.CommentDTO;
import com.collab.entity.Comment;
import com.collab.vo.CommentVO;


import java.util.List;

public interface CommentService
        extends IService<Comment> {

    void addComment(CommentDTO dto);

    List<CommentVO> listComment(Long taskId);

    void deleteComment(Long id);

}