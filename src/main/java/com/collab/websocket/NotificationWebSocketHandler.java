package com.collab.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     * 在线用户
     */
    public static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    /**
     * JSON工具
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 用户连接
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String query = session.getUri().getQuery();

        // ws://localhost:8080/ws/notification?userId=1
        Long userId = Long.parseLong(query.split("=")[1]);

        ONLINE_USERS.put(userId, session);

        log.info("用户上线：{}", userId);
    }

    /**
     * 接收客户端消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("收到客户端消息：{}", message.getPayload());
    }

    /**
     * 用户断开
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ONLINE_USERS.values().remove(session);
        log.info("用户离线");
    }

    /**
     * 发送文本消息
     */
    public static void sendMessage(Long userId, NotificationMessage message) {
        try {
            WebSocketSession session = ONLINE_USERS.get(userId);

            if (session != null && session.isOpen()) {

                String json = OBJECT_MAPPER.writeValueAsString(message);

                session.sendMessage(new TextMessage(json));
                log.info("WebSocket推送成功 -> userId={}", userId);
            }

        } catch (Exception e) {
            log.error("WebSocket发送失败", e);
        }
    }
}