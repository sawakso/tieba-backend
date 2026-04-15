package chat.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import java.util.List;  // ✅ 添加这个导入
import java.util.concurrent.CompletableFuture;

/**
 * Kafka 消息生产者服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // 主题名称（建议定义为常量）
    private static final String CHAT_MESSAGE_TOPIC = "chat-messages";

    /**
     * 发送聊天消息到 Kafka
     * @param messageJson 消息JSON字符串
     */
    public void sendChatMessage(String messageJson) {
        sendMessage(CHAT_MESSAGE_TOPIC, messageJson);
    }

    /**
     * 发送消息到指定主题（同步发送）
     * @param topic 主题名称
     * @param message 消息内容
     */
    public void sendMessage(String topic, String message) {
        try {
            // 同步发送
            SendResult<String, String> result = kafkaTemplate.send(topic, message).get();

            log.info("✅ Kafka消息发送成功: topic={}, offset={}, partition={}, message={}",
                    topic,
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().partition(),
                    truncateMessage(message));

        } catch (Exception e) {
            log.error("❌ Kafka消息发送失败: topic={}, message={}, error={}",
                    topic, truncateMessage(message), e.getMessage());
            // 这里可以添加重试逻辑或保存到失败队列
            handleSendFailure(topic, message, e);
        }
    }

    /**
     * 发送消息到指定主题（异步发送 - 推荐）
     * @param topic 主题名称
     * @param message 消息内容
     */
    public void sendMessageAsync(String topic, String message) {
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("✅ Kafka消息发送成功: topic={}, offset={}, partition={}",
                        topic,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("❌ Kafka消息发送失败: topic={}, error={}", topic, ex.getMessage());
                handleSendFailure(topic, message, ex);
            }
        });
    }

    /**
     * 发送聊天消息（异步方式 - 推荐用于聊天系统）
     * @param messageJson 消息JSON字符串
     */
    public void sendChatMessageAsync(String messageJson) {
        sendMessageAsync(CHAT_MESSAGE_TOPIC, messageJson);
    }

    /**
     * 发送消息并返回 CompletableFuture（支持更灵活的异步处理）
     * @param topic 主题名称
     * @param key 消息Key（用于分区）
     * @param message 消息内容
     * @return CompletableFuture
     */
    public CompletableFuture<SendResult<String, String>> sendMessageWithKey(String topic, String key, String message) {
        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, key, message).completable();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Kafka消息发送成功: topic={}, key={}, offset={}, partition={}",
                        topic, key,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            } else {
                log.error("❌ Kafka消息发送失败: topic={}, key={}, error={}",
                        topic, key, ex.getMessage());
                handleSendFailure(topic, message, ex);
            }
        });

        return future;
    }

    /**
     * 发送聊天消息（带Key，确保同一会话的消息进入同一分区）
     * @param messageJson 消息JSON字符串
     * @param conversationId 会话ID作为Key
     */
    public void sendChatMessageWithKey(String messageJson, String conversationId) {
        sendMessageWithKey(CHAT_MESSAGE_TOPIC, conversationId, messageJson);
    }

    /**
     * 批量发送消息
     * @param topic 主题名称
     * @param messages 消息列表
     */
    public void sendBatchMessages(String topic, List<String> messages) {
        if (messages == null || messages.isEmpty()) {  // ✅ isEmpty() 方法
            log.warn("⚠️ 批量发送消息列表为空，跳过发送");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (String message : messages) {
            try {
                kafkaTemplate.send(topic, message);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("❌ 批量发送单条消息失败: topic={}, error={}", topic, e.getMessage());
            }
        }

        log.info("📊 批量发送完成: topic={}, 成功={}, 失败={}", topic, successCount, failCount);
    }

    /**
     * 发送带回调的消息（同步等待）
     * @param topic 主题名称
     * @param message 消息内容
     * @param timeoutMillis 超时时间（毫秒）
     * @return 是否发送成功
     */
    public boolean sendMessageSync(String topic, String message, long timeoutMillis) {
        try {
            SendResult<String, String> result =
                    kafkaTemplate.send(topic, message).get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);

            log.info("✅ Kafka同步消息发送成功: topic={}, offset={}",
                    topic, result.getRecordMetadata().offset());
            return true;

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("⏰ Kafka消息发送超时: topic={}, timeout={}ms", topic, timeoutMillis);
            return false;
        } catch (Exception e) {
            log.error("❌ Kafka消息发送失败: topic={}, error={}", topic, e.getMessage());
            return false;
        }
    }

    /**
     * 检查 Kafka 是否可用
     * @return true-可用，false-不可用
     */
    public boolean isKafkaAvailable() {
        try {
            // 发送一个测试消息来检查连接
            kafkaTemplate.send(CHAT_MESSAGE_TOPIC, "test-connection");
            return true;
        } catch (Exception e) {
            log.error("❌ Kafka 连接不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理发送失败的情况
     * @param topic 主题
     * @param message 消息内容
     * @param e 异常
     */
    private void handleSendFailure(String topic, String message, Throwable e) {
        // 方案1: 保存到数据库失败表，后续重试
        // saveToFailureTable(topic, message, e.getMessage());

        // 方案2: 发送到死信队列
        // sendToDeadLetterQueue(topic, message);

        // 方案3: 记录到日志文件
        log.warn("⚠️ 消息发送失败，需人工处理: topic={}, message={}", topic, truncateMessage(message));

        // 方案4: 抛出异常让上层处理（如果需要事务回滚）
        // throw new RuntimeException("Kafka message send failed", e);
    }

    /**
     * 截断长消息用于日志输出
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "null";
        }
        return message.length() > 100 ? message.substring(0, 97) + "..." : message;
    }
}