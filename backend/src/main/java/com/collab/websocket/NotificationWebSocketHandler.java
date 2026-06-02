package com.collab.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket通知处理器
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     * 保存用户 websocket session
     *
     * key:
     * userId
     *
     * value:
     * WebSocketSession
     */
    private static final Map<Long, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 建立连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        Long userId = getUserId(query);
        /**
         * 保存 session
         */
        SESSION_MAP.put(userId, session);

        log.info("====================================");

        log.info("WebSocket连接成功");

        log.info("userId = {}", userId);

        log.info("sessionId = {}", session.getId());

        log.info("当前在线人数 = {}", SESSION_MAP.size());

        log.info("当前在线用户 = {}", SESSION_MAP.keySet());

        log.info("====================================");
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSION_MAP.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));

        log.info("====================================");

        log.info("WebSocket连接关闭");

        log.info("sessionId = {}", session.getId());

        log.info("当前在线人数 = {}", SESSION_MAP.size());

        log.info("当前在线用户 = {}", SESSION_MAP.keySet());

        log.info("====================================");
    }

    /**
     * 接收前端消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到客户端消息：{}", message.getPayload());
    }

    /**
     * websocket异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

        log.error("WebSocket异常");

        log.error("sessionId = {}", session.getId());

        log.error("异常信息", exception);
    }

    /**
     * 发送消息
     */
    public static void sendMessage(Long userId, NotificationMessage message) {
        log.info("====================================");

        log.info("准备发送WebSocket消息");

        log.info("目标userId = {}", userId);

        log.info("消息内容 = {}", message);

        WebSocketSession session = SESSION_MAP.get(userId);

        log.info("获取到session = {}", session);

        if (
                session != null && session.isOpen()
        ) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                log.info("WebSocket消息发送成功");

                log.info("userId = {}", userId);

                log.info("sessionId = {}", session.getId());

            } catch (Exception e) {

                log.error("WebSocket消息发送失败", e);
            }

        } else {

            log.warn("用户不在线或session已关闭");

            log.warn("userId = {}", userId);
        }

        log.info("====================================");
    }

    /**
     * 从url参数获取userId
     *
     * ws://localhost:8080/ws/notification?userId=1
     */
    private Long getUserId(String query) {

        if (query == null || !query.contains("userId=")) {

            return null;
        }

        String[] params = query.split("&");

        for (String param : params) {

            if (param.startsWith("userId=")) {

                return Long.parseLong(
                        param.replace("userId=", "")
                );
            }
        }

        return null;
    }
}