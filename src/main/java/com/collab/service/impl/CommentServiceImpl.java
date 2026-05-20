package com.collab.service.impl;

import com.collab.entity.Task;
import com.collab.mapper.TaskMapper;
import com.collab.websocket.NotificationMessage;
import com.collab.websocket.NotificationWebSocketHandler;
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

    private final TaskMapper taskMapper;

    @Override
    public void addComment(CommentDTO dto) {

        Comment comment = new Comment();

        BeanUtils.copyProperties(dto, comment);

        comment.setUserId(LoginUserContext.getUserId());

        // 一级评论
        if (comment.getParentId() == null) {

            comment.setParentId(0L);
        }

        commentMapper.insert(comment);

        /**
         * 查询任务
         */
        Task task = taskMapper.selectById(comment.getTaskId());

        if (task != null) {

            /**
             * 通知任务创建人
             */
            if (!task.getCreatorId().equals(comment.getUserId())) {
                NotificationMessage message = new NotificationMessage(
                                "COMMENT",
                                "任务收到新评论：" + task.getTitle(),
                                task.getId(),
                                System.currentTimeMillis()
                        );
                NotificationWebSocketHandler.sendMessage(
                        task.getCreatorId(),
                        message
                );
            }

            /**
             * 通知任务执行人
             */
            if (task.getExecutorId() != null && !task.getExecutorId().equals(comment.getUserId())) {
                NotificationMessage message = new NotificationMessage(
                                "COMMENT",
                                "任务收到新评论：" + task.getTitle(),
                                task.getId(),
                                System.currentTimeMillis()
                        );

                NotificationWebSocketHandler.sendMessage(
                        task.getExecutorId(),
                        message
                );
            }
        }
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