package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.CommentDTO;
import com.collab.entity.Comment;
import com.collab.entity.User;
import com.collab.mapper.CommentMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.CommentService;
import com.collab.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;

    private final UserMapper userMapper;

    @Override
    public void addComment(CommentDTO dto) {

        Comment comment = new Comment();

        BeanUtils.copyProperties(dto, comment);

        // 当前登录用户ID
        //comment.setUserId(1L);
        comment.setUserId(LoginUserContext.getUserId());

        // 一级评论
        if (comment.getParentId() == null) {

            comment.setParentId(0L);
        }

        commentMapper.insert(comment);
    }

    @Override
    public List<CommentVO> listComment(Long taskId) {

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Comment::getTaskId, taskId);

        wrapper.orderByAsc(Comment::getCreateTime);

        List<Comment> comments = commentMapper.selectList(wrapper);

        return comments.stream()
                .map(this::buildCommentVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long id) {

        // 删除当前评论
        commentMapper.deleteById(id);

        // 删除回复评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Comment::getParentId, id);

        commentMapper.delete(wrapper);
    }

    /**
     * 封装 CommentVO
     */
    private CommentVO buildCommentVO(Comment comment) {

        CommentVO vo = new CommentVO();

        BeanUtils.copyProperties(comment, vo);

        User user = userMapper.selectById(comment.getUserId());

        if (user != null) {

            vo.setNickname(user.getNickname());

            vo.setAvatar(user.getAvatar());
        }

        if (comment.getCreateTime() != null) {

            vo.setCreateTime(comment.getCreateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        }

        return vo;
    }
}