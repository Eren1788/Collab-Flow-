package com.collab.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.common.utils.LoginUserContext;
import com.collab.dto.CommentDTO;
import com.collab.entity.*;
import com.collab.mapper.*;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final NotificationService notificationService;
    private final ProjectActivityService projectActivityService;
    private final TaskExecutorMapper taskExecutorMapper;

    // ========== 原有：任务评论（完全保留，未修改） ==========
    @Override
    public void addComment(CommentDTO dto) throws JsonProcessingException {
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

            // 查询该任务的所有执行人，逐个通知
            LambdaQueryWrapper<TaskExecutor> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskExecutor::getTaskId, task.getId());
            List<TaskExecutor> executors = taskExecutorMapper.selectList(wrapper);
            for (TaskExecutor te : executors) {
                Long executorId = te.getUserId();
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

            // WebSocket 推送新评论消息（用于实时聊天）
            ObjectMapper mapper = new ObjectMapper();
            CommentVO commentVO = buildCommentVO(comment);
            commentVO.setType("TASK_COMMENT");  // 标记类型
            String wsMessage = mapper.writeValueAsString(Map.of(
                    "type", "NEW_COMMENT",
                    "taskId", task.getId(),
                    "comment", commentVO
            ));
            NotificationMessage wsNotification = new NotificationMessage(
                    "NEW_COMMENT",
                    wsMessage,
                    task.getId(),
                    System.currentTimeMillis()
            );
            if (!task.getCreatorId().equals(comment.getUserId())) {
                NotificationWebSocketHandler.sendMessage(task.getCreatorId(), wsNotification);
            }
            for (TaskExecutor te : executors) {
                Long userId = te.getUserId();
                if (!userId.equals(comment.getUserId()) && !userId.equals(task.getCreatorId())) {
                    NotificationWebSocketHandler.sendMessage(userId, wsNotification);
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
                .map(comment -> {
                    CommentVO vo = buildCommentVO(comment);
                    vo.setType("TASK_COMMENT");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, id);
        commentMapper.delete(wrapper);
    }

    // ========== 新增：项目评论 ==========
    @Override
    public void addProjectComment(CommentDTO dto) throws JsonProcessingException {
        Comment comment = new Comment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(LoginUserContext.getUserId());
        comment.setTaskId(null);  // 确保不是任务评论
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        commentMapper.insert(comment);

        // 项目动态
        ProjectActivity activity = new ProjectActivity();
        activity.setProjectId(comment.getProjectId());
        activity.setUserId(comment.getUserId());
        activity.setType("PROJECT_COMMENT");
        activity.setContent("在项目聊天室发言：" + comment.getContent());
        projectActivityService.addActivity(activity);

        // 查询该项目所有成员
        LambdaQueryWrapper<ProjectMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ProjectMember::getProjectId, comment.getProjectId());
        List<ProjectMember> members = projectMemberMapper.selectList(memberWrapper);

        String content = String.format("项目聊天室有新消息：%s",
                comment.getContent().length() > 30 ? comment.getContent().substring(0,30) + "..." : comment.getContent()
        );

        // 构建 CommentVO 用于 WebSocket 推送
        CommentVO commentVO = buildCommentVO(comment);
        commentVO.setType("PROJECT_COMMENT");
        ObjectMapper mapper = new ObjectMapper();
        String wsMessage = mapper.writeValueAsString(Map.of(
                "type", "NEW_PROJECT_COMMENT",
                "projectId", comment.getProjectId(),
                "comment", commentVO
        ));
        NotificationMessage wsNotification = new NotificationMessage(
                "NEW_PROJECT_COMMENT",
                wsMessage,
                comment.getProjectId(),
                System.currentTimeMillis()
        );

        // 通知所有项目成员（除了自己）
        for (ProjectMember member : members) {
            Long memberId = member.getUserId();
            if (!memberId.equals(comment.getUserId())) {
                notificationService.saveNotification(
                        memberId,
                        comment.getUserId(),
                        "PROJECT_COMMENT",
                        content,
                        comment.getProjectId()
                );
                NotificationWebSocketHandler.sendMessage(memberId, wsNotification);
            }
        }
    }

    @Override
    public List<CommentVO> listProjectComment(Long projectId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getProjectId, projectId);
        wrapper.orderByAsc(Comment::getCreateTime);
        List<Comment> comments = commentMapper.selectList(wrapper);
        return comments.stream()
                .map(comment -> {
                    CommentVO vo = buildCommentVO(comment);
                    vo.setType("PROJECT_COMMENT");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ========== 公共方法 ==========
    private CommentVO buildCommentVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        vo.setUserId(comment.getUserId());
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        if (comment.getCreateTime() != null) {
            vo.setCreateTime(comment.getCreateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return vo;
    }
}