package chat.kafka;

import chat.domain.dto.ChatMessageDTO;
import chat.service.ChatMessageService;
import chat.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ObjectMapper objectMapper;
    private final ChatWebSocketHandler webSocketHandler;
    private final ChatMessageService messageService;

    @KafkaListener(topics = "chat-messages", groupId = "chat-group")
    public void consumeMessage(String messageJson) {
        try {
            log.info("📥 从 Kafka 消费到消息: {}", messageJson);

            // 1. 解析消息
            ChatMessageDTO msgDTO = objectMapper.readValue(messageJson, ChatMessageDTO.class);

            // 2. 保存到数据库（这里可以跳过，因为 Producer 已经保存了）
            // messageService.saveMessage(msgDTO);  // 避免重复保存

            // 3. 推送给在线用户
            webSocketHandler.sendMessageToUser(msgDTO.getToUserId(), "CHAT_MESSAGE", msgDTO);

        } catch (Exception e) {
            log.error("❌ 处理 Kafka 消息失败: {}", e.getMessage(), e);
        }
    }
}