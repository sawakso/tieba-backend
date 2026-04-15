package chat.websocket;

import chat.domain.dto.ChatMessageDTO;
import chat.domain.dto.Result;
import chat.feign.UserFriendClient;
import chat.service.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatMessageService messageService;
    private final UserFriendClient userFriendClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;  // 新增

    // 存储所有在线用户的 session
    private static final Map<String, WebSocketSession> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_SESSIONS.put(userId, session);
            log.info("✅ WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());

            // 发送连接成功消息
            sendMessage(session, "CONNECTED", "连接成功");

            // 推送离线消息
            messageService.getOfflineMessages(userId).forEach(msg -> {
                try {
                    sendMessage(session, "OFFLINE_MESSAGE", msg);
                } catch (IOException e) {
                    log.error("推送离线消息失败", e);
                }
            });
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String currentUserId = (String) session.getAttributes().get("userId");
        String payload = (String) message.getPayload();

        log.info("📨 收到消息: userId={}, payload={}", currentUserId, payload);

        try {
            ChatMessageDTO msgDTO = objectMapper.readValue(payload, ChatMessageDTO.class);
            msgDTO.setFromUserId(currentUserId);

            if (!checkMessagePermission(currentUserId, msgDTO.getToUserId())) {
                sendErrorMessage(session, "你们还不是好友，无法发送消息");
                return;
            }

            // 1. 保存消息到数据库
            messageService.saveMessage(msgDTO);

            // 2. 🔥 写入 Kafka
            try {
                String jsonMsg = objectMapper.writeValueAsString(msgDTO);
                kafkaTemplate.send("chat-messages", msgDTO.getFromUserId(), jsonMsg);
                log.info("✅ 消息已写入 Kafka: {}", msgDTO.getContent());
            } catch (Exception e) {
                log.error("❌ Kafka 写入失败", e);
            }

            // 3. 转发消息给目标用户
            sendMessageToUser(msgDTO.getToUserId(), "CHAT_MESSAGE", msgDTO);

            // 4. 发送确认消息给发送者
            sendMessage(session, "MESSAGE_SENT", "消息发送成功");

        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_SESSIONS.remove(userId);
            log.info("❌ WebSocket 连接关闭: userId={}, status={}", userId, closeStatus);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 检查发送消息的权限（好友验证）
     */
    private boolean checkMessagePermission(String fromUserId, String toUserId) {
        // 自己发给自己，允许
        if (fromUserId.equals(toUserId)) {
            return true;
        }

        try {
            Long fromId = Long.valueOf(fromUserId);
            Long toId = Long.valueOf(toUserId);

            Result result = userFriendClient.checkFriendship(fromId, toId);

            if (!result.getSuccess()) {
                log.warn("好友关系检查失败: {}", result.getErrorMsg());
                return false;
            }

            Map<String, Object> data = (Map<String, Object>) result.getData();
            Boolean isFriend = (Boolean) data.get("isFriend");

            return isFriend != null && isFriend;

        } catch (Exception e) {
            log.error("调用 user-server 检查好友关系失败", e);
            return false;
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(String targetUserId, String type, Object data) throws IOException {
        WebSocketSession targetSession = ONLINE_SESSIONS.get(targetUserId);
        if (targetSession != null && targetSession.isOpen()) {
            sendMessage(targetSession, type, data);
        } else {
            log.info("用户 {} 不在线，消息已保存到数据库", targetUserId);
        }
    }

    /**
     * 发送消息给指定 session
     */
    private void sendMessage(WebSocketSession session, String type, Object data) throws IOException {
        Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        String jsonMessage = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMsg) throws IOException {
        Map<String, Object> errorMessage = Map.of(
                "type", "ERROR",
                "message", errorMsg,
                "timestamp", System.currentTimeMillis()
        );
        String jsonMessage = objectMapper.writeValueAsString(errorMessage);
        session.sendMessage(new TextMessage(jsonMessage));
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineCount() {
        return ONLINE_SESSIONS.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = ONLINE_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }
    /**
     * 获取在线用户列表
     */
    public static Map<String, WebSocketSession> getOnlineUsers() {
        return ONLINE_SESSIONS;
    }

    /**
     * 获取在线用户ID集合
     */
    public Set<String> getOnlineUserIds() {
        return ONLINE_SESSIONS.keySet();
    }
}