package com.collab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.entity.Comment;
import com.collab.mapper.CommentMapper;
import com.collab.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
}
