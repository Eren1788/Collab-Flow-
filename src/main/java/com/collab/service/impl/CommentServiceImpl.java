package com.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.CommentDTO;
import com.collab.entity.Comment;
import com.collab.entity.ProjectActivity;
import com.collab.entity.Task;
import com.collab.entity.TaskExecutor;
import com.collab.entity.User;
import com.collab.mapper.CommentMapper;
import com.collab.mapper.TaskExecutorMapper;
import com.collab.mapper.TaskMapper;
import com.collab.mapper.UserMapper;
import com.collab.service.CommentService;
import com.collab.service.NotificationService;
import com.collab.service.ProjectActivityService;
import com.collab.vo.CommentVO;
import com.collab.websocket.NotificationMessage;
import com.collab.websocket.NotificationWebSocketHandler;
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
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;
    private final TaskExecutorMapper taskExecutorMapper;  // 新增：用于查询多执行人

    @Override
    public void addComment(CommentDTO dto) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(LoginUserContext.getUserId());

        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }

        commentMapper.insert(comment);

        Task task = taskMapper.selectById(comment.getTaskId());
        if (task != null) {
            // 项目动态
            ProjectActivity activity = new ProjectActivity();
            activity.setProjectId(task.getProjectId());
            activity.setUserId(comment.getUserId());
            activity.setType("COMMENT");
            activity.setContent("发表评论：" + comment.getContent());
            projectActivityService.addActivity(activity);

            String content = "任务收到新评论：" + task.getTitle();

            // 通知任务创建人（如果创建人不是评论人自己）
            if (!task.getCreatorId().equals(comment.getUserId())) {
                notificationService.saveNotification(
                        task.getCreatorId(),
                        comment.getUserId(),
                        "COMMENT",
                        content,
                        task.getId()
                );
                NotificationMessage message = new NotificationMessage(
                        "COMMENT",
                        content,
                        task.getId(),
                        System.currentTimeMillis()
                );
                NotificationWebSocketHandler.sendMessage(task.getCreatorId(), message);
            }

            // 查询该任务的所有执行人，逐个通知（如果执行人不是评论人自己且不是创建人）
            LambdaQueryWrapper<TaskExecutor> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskExecutor::getTaskId, task.getId());
            List<TaskExecutor> executors = taskExecutorMapper.selectList(wrapper);
            for (TaskExecutor te : executors) {
                Long executorId = te.getUserId();
                // 避免重复通知创建人（如果创建人也作为执行人已通知过）
                if (!executorId.equals(comment.getUserId()) && !executorId.equals(task.getCreatorId())) {
                    notificationService.saveNotification(
                            executorId,
                            comment.getUserId(),
                            "COMMENT",
                            content,
                            task.getId()
                    );
                    NotificationMessage message = new NotificationMessage(
                            "COMMENT",
                            content,
                            task.getId(),
                            System.currentTimeMillis()
                    );
                    NotificationWebSocketHandler.sendMessage(executorId, message);
                }
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
        commentMapper.deleteById(id);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, id);
        commentMapper.delete(wrapper);
    }

    private CommentVO buildCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        if (comment.getCreateTime() != null) {
            vo.setCreateTime(
                    comment.getCreateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
        }
        return vo;
    }
}