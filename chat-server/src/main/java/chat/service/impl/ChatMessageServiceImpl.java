package chat.service.impl;

import chat.domain.dto.ChatMessageDTO;
import chat.domain.dto.Result;
import chat.domain.pojo.ChatMessage;
import chat.feign.UserFriendClient;
import chat.mapper.ChatMessageMapper;
import chat.service.ChatMessageService;
import chat.service.ChatConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements ChatMessageService {

    private final ChatMessageMapper messageMapper;
    private final ChatConversationService conversationService;
    private final UserFriendClient userFriendClient;  // 注入好友服务客户端

    @Override
    @Transactional
    public void saveMessage(ChatMessageDTO msgDTO) {
        // 1. 检查是否有权限发送消息（好友验证）
        if (!checkMessagePermission(msgDTO.getFromUserId(), msgDTO.getToUserId())) {
            log.warn("❌ 消息发送失败: 用户 {} 和 {} 不是好友关系",
                    msgDTO.getFromUserId(), msgDTO.getToUserId());
            throw new RuntimeException("你们还不是好友，无法发送消息");
        }

        // 2. 生成消息ID（使用 MP 自带的雪花算法）
        String msgId = IdWorker.getIdStr();  // 返回 String 类型的雪花ID

        // 3. 构建消息实体
        ChatMessage message = new ChatMessage();
        message.setMsgId(msgId);
        message.setFromUserId(msgDTO.getFromUserId());
        message.setToUserId(msgDTO.getToUserId());
        message.setContentType(msgDTO.getContentType() != null ? msgDTO.getContentType() : 1);
        message.setContent(msgDTO.getContent());
        message.setStatus(2); // 2-已送达
        message.setSendTime(LocalDateTime.now());
        message.setDeliverTime(LocalDateTime.now());

        // 4. 保存消息
        this.save(message);

        // 5. 更新双方的会话记录
        conversationService.updateConversation(message);

        log.info("💾 消息已保存: msgId={}, from={}, to={}", msgId,
                msgDTO.getFromUserId(), msgDTO.getToUserId());
    }

    @Override
    public List<ChatMessage> getOfflineMessages(String userId) {
        return messageMapper.findOfflineMessages(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String msgId, String readerId) {
        // 使用 MP 的 LambdaUpdateWrapper
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMessage::getMsgId, msgId)
                .set(ChatMessage::getStatus, 3)
                .set(ChatMessage::getReadTime, LocalDateTime.now());

        this.update(wrapper);

        // 获取消息详情，更新会话未读数
        ChatMessage message = this.getOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getMsgId, msgId));

        if (message != null) {
            // 清空该会话的未读数
            conversationService.clearUnreadCount(readerId, message.getFromUserId());
        }

        log.info("✅ 消息 {} 已被用户 {} 标记为已读", msgId, readerId);
    }

    @Override
    @Transactional
    public void batchMarkAsDelivered(List<String> msgIds) {
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(ChatMessage::getMsgId, msgIds)
                .set(ChatMessage::getStatus, 2)
                .set(ChatMessage::getDeliverTime, LocalDateTime.now());

        this.update(wrapper);
    }

    @Override
    public Page<ChatMessage> getConversationMessages(String userId1, String userId2,
                                                     long current, long size) {
        // 检查是否有权限查看聊天记录（好友验证）
        if (!checkMessagePermission(userId1, userId2)) {
            log.warn("❌ 获取聊天记录失败: 用户 {} 和 {} 不是好友关系", userId1, userId2);
            throw new RuntimeException("你们还不是好友，无法查看聊天记录");
        }

        // 使用 MP 的分页
        Page<ChatMessage> page = new Page<>(current, size);

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                        .and(w1 -> w1.eq(ChatMessage::getFromUserId, userId1)
                                .eq(ChatMessage::getToUserId, userId2))
                        .or(w2 -> w2.eq(ChatMessage::getFromUserId, userId2)
                                .eq(ChatMessage::getToUserId, userId1)))
                .orderByDesc(ChatMessage::getSendTime);

        return this.page(page, wrapper);
    }

    /**
     * 检查发送消息的权限（私聊需要是好友）
     */
    private boolean checkMessagePermission(String fromUserId, String toUserId) {
        // 自己发给自己，允许
        if (fromUserId.equals(toUserId)) {
            return true;
        }

        try {
            // 转换为 Long 类型
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
     * 获取用户的好友ID列表（用于群发消息等场景）
     */
    public Set<String> getFriendIds(String userId) {
        try {
            Long uid = Long.valueOf(userId);
            Result result = userFriendClient.getFriendIds(uid);

            if (!result.getSuccess()) {
                log.warn("获取好友列表失败: {}", result.getErrorMsg());
                return new HashSet<>();
            }

            Map<String, Object> data = (Map<String, Object>) result.getData();
            Set<Long> friendIds = (Set<Long>) data.get("friendIds");

            // 转换为 String 类型返回
            Set<String> friendIdStrings = new HashSet<>();
            if (friendIds != null) {
                for (Long id : friendIds) {
                    friendIdStrings.add(String.valueOf(id));
                }
            }
            return friendIdStrings;

        } catch (Exception e) {
            log.error("调用 user-server 获取好友列表失败", e);
            return new HashSet<>();
        }
    }
}