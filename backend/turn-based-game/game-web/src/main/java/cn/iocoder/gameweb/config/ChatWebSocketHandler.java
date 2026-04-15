package cn.iocoder.gameweb.config;

import cn.iocoder.gamemodules.entity.ChatContent;
import cn.iocoder.gamemodules.service.ChatService;
import cn.iocoder.gamecommon.util.JwtUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天WebSocket处理器
 * 处理实时消息推送
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 存储用户会话：userId -> WebSocketSession
     */
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 存储频道订阅：channelId -> Map<userId, WebSocketSession>
     */
    private static final Map<Long, Map<Long, WebSocketSession>> CHANNEL_SUBSCRIBERS = new ConcurrentHashMap<>();

    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("WebSocket连接未携带有效的用户Token");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 存储用户会话
        USER_SESSIONS.put(userId, session);
        log.info("用户 {} 建立WebSocket连接，当前在线用户数: {}", userId, USER_SESSIONS.size());

        // 发送连接成功消息
        sendMessage(session, createMessage("connected", "连接成功", null));
    }

    /**
     * 收到文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            sendMessage(session, createMessage("error", "未登录", null));
            return;
        }

        try {
            String payload = message.getPayload();
            JSONObject json = JSON.parseObject(payload);
            String type = json.getString("type");

            switch (type) {
                case "subscribe_channel":
                    // 订阅频道
                    Long channelId = json.getLong("channelId");
                    subscribeChannel(userId, channelId, session);
                    break;
                case "unsubscribe_channel":
                    // 取消订阅频道
                    Long unsubChannelId = json.getLong("channelId");
                    unsubscribeChannel(userId, unsubChannelId);
                    break;
                case "send_channel_message":
                    // 发送频道消息
                    handleSendChannelMessage(userId, json);
                    break;
                case "send_private_message":
                    // 发送私聊消息
                    handleSendPrivateMessage(userId, json);
                    break;
                case "ping":
                    // 心跳响应
                    sendMessage(session, createMessage("pong", "pong", null));
                    break;
                default:
                    sendMessage(session, createMessage("error", "未知消息类型: " + type, null));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendMessage(session, createMessage("error", "消息处理失败: " + e.getMessage(), null));
        }
    }

    /**
     * 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            // 移除用户会话
            USER_SESSIONS.remove(userId);
            // 从所有频道中移除
            CHANNEL_SUBSCRIBERS.forEach((channelId, subscribers) -> {
                subscribers.remove(userId);
            });
            log.info("用户 {} 断开WebSocket连接，当前在线用户数: {}", userId, USER_SESSIONS.size());
        }
    }

    /**
     * 从Session中获取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || !query.contains("token=")) {
                return null;
            }
            String token = query.substring(query.indexOf("token=") + 6);
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("解析Token失败", e);
            return null;
        }
    }

    /**
     * 订阅频道
     */
    private void subscribeChannel(Long userId, Long channelId, WebSocketSession session) {
        CHANNEL_SUBSCRIBERS.computeIfAbsent(channelId, k -> new ConcurrentHashMap<>()).put(userId, session);
        sendMessage(session, createMessage("subscribed", "已订阅频道 " + channelId, 
                Map.of("channelId", channelId)));
        log.info("用户 {} 订阅频道 {}", userId, channelId);
    }

    /**
     * 取消订阅频道
     */
    private void unsubscribeChannel(Long userId, Long channelId) {
        Map<Long, WebSocketSession> subscribers = CHANNEL_SUBSCRIBERS.get(channelId);
        if (subscribers != null) {
            subscribers.remove(userId);
            log.info("用户 {} 取消订阅频道 {}", userId, channelId);
        }
    }

    /**
     * 处理发送频道消息
     */
    private void handleSendChannelMessage(Long userId, JSONObject json) {
        Long channelId = json.getLong("channelId");
        String content = json.getString("content");
        String msgUuid = json.getString("msgUuid");

        if (channelId == null || content == null || content.trim().isEmpty()) {
            return;
        }

        // 保存消息到数据库
        var result = chatService.sendChannelMessage(userId, channelId, content, msgUuid);
        if (result.getCode() == 200) {
            ChatContent message = result.getData();
            // 广播给频道内所有订阅者
            broadcastToChannel(channelId, createMessage("channel_message", "新消息",
                    Map.of("message", message)));
        }
    }

    /**
     * 处理发送私聊消息
     */
    private void handleSendPrivateMessage(Long userId, JSONObject json) {
        Long receiverId = json.getLong("receiverId");
        String content = json.getString("content");
        String msgUuid = json.getString("msgUuid");

        if (receiverId == null || content == null || content.trim().isEmpty()) {
            return;
        }

        // 保存消息到数据库
        var result = chatService.sendPrivateMessage(userId, receiverId, content, msgUuid);
        if (result.getCode() == 200) {
            ChatContent message = result.getData();
            // 发送给发送者
            WebSocketSession senderSession = USER_SESSIONS.get(userId);
            if (senderSession != null && senderSession.isOpen()) {
                sendMessage(senderSession, createMessage("private_message_sent", "发送成功",
                        Map.of("message", message)));
            }
            // 发送给接收者
            WebSocketSession receiverSession = USER_SESSIONS.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                sendMessage(receiverSession, createMessage("private_message", "新私聊消息",
                        Map.of("message", message)));
            }
        }
    }

    /**
     * 广播消息给频道内所有订阅者
     */
    private void broadcastToChannel(Long channelId, String message) {
        Map<Long, WebSocketSession> subscribers = CHANNEL_SUBSCRIBERS.get(channelId);
        if (subscribers != null) {
            subscribers.forEach((userId, session) -> {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            });
        }
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 创建消息JSON
     */
    private String createMessage(String type, String message, Object data) {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("message", message);
        json.put("data", data);
        json.put("timestamp", System.currentTimeMillis());
        return json.toJSONString();
    }
}
