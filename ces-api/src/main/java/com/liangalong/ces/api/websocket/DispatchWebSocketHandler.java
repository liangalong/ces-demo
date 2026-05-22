package com.liangalong.ces.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangalong.ces.core.scheduler.TaskSchedulerEngine;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 调度事件推送
 * 实时推送任务分配/完成/超时等事件到前端
 */
@Component
public class DispatchWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DispatchWebSocketHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final TaskSchedulerEngine engine;

    public DispatchWebSocketHandler(TaskSchedulerEngine engine) {
        this.engine = engine;
    }

    @PostConstruct
    public void init() {
        engine.addEventListener((type, data) -> {
            try {
                String msg = mapper.writeValueAsString(Map.of(
                        "type", type,
                        "data", data,
                        "stats", engine.getStats()
                ));
                broadcast(msg);
            } catch (Exception e) {
                log.warn("WS序列化失败: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("🔗 WS连接: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("🔌 WS断开: {} ({})", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 客户端消息暂不处理
    }

    private void broadcast(String msg) {
        TextMessage tm = new TextMessage(msg);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(tm);
                }
            } catch (Exception e) {
                log.warn("WS发送失败: {}", e.getMessage());
            }
        }
    }
}
