package chat.service.impl;

import chat.domain.pojo.ChatConversation;
import chat.domain.pojo.ChatMessage;
import chat.mapper.ChatConversationMapper;
import chat.service.ChatConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl
        extends ServiceImpl<ChatConversationMapper, ChatConversation>
        implements ChatConversationService {

    private final ChatConversationMapper conversationMapper;

    @Override
    public List<ChatConversation> getUserConversations(String userId) {
        // 只查询未删除的会话
        List<ChatConversation> conversations = conversationMapper.findUserConversations(userId);

        // 过滤掉已删除的会话
        conversations.removeIf(conv -> conv.getIsDeleted() != null && conv.getIsDeleted() == 1);

        log.debug("📋 获取用户 {} 的会话列表，共 {} 个会话", userId, conversations.size());
        return conversations;
    }

    @Override
    @Transactional
    public void updateConversation(ChatMessage message) {
        String conversationId = generateConversationId(
                message.getFromUserId(),
                message.getToUserId()
        );

        LocalDateTime now = LocalDateTime.now();

        // 1. 更新发送者的会话（未读数不变，但更新最后消息时间）
        upsertConversation(
                conversationId,
                message.getFromUserId(),
                message.getToUserId(),
                message.getMsgId(),
                message.getContent(),
                now,
                0,  // 发送者未读数不变
                false  // 不增加未读数
        );

        // 2. 更新接收者的会话（未读数+1）
        upsertConversation(
                conversationId,
                message.getToUserId(),
                message.getFromUserId(),
                message.getMsgId(),
                message.getContent(),
                now,
                1,  // 接收者未读数+1
                true  // 需要增加未读数
        );

        log.info("💬 会话已更新: conversationId={}, from={}, to={}",
                conversationId, message.getFromUserId(), message.getToUserId());
    }

    /**
     * 插入或更新会话
     * @param conversationId 会话ID
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     * @param msgId 最新消息ID
     * @param content 消息内容
     * @param time 消息时间
     * @param unreadDelta 未读数增量
     * @param incrementUnread 是否增加未读数
     */
    private void upsertConversation(String conversationId, String userId, String targetId,
                                    String msgId, String content, LocalDateTime time,
                                    int unreadDelta, boolean incrementUnread) {

        // 1. 查询现有会话
        ChatConversation conversation = this.getConversation(userId, targetId);

        if (conversation == null) {
            // 2. 不存在，创建新会话
            conversation = new ChatConversation();
            conversation.setConversationId(conversationId);
            conversation.setUserId(userId);
            conversation.setTargetId(targetId);
            conversation.setLastMsgId(msgId);
            conversation.setLastMsgContent(truncateContent(content));
            conversation.setLastMsgTime(time);
            conversation.setUnreadCount(incrementUnread ? unreadDelta : 0);
            conversation.setIsTop(0);
            conversation.setIsMuted(0);
            conversation.setIsDeleted(0);

            this.save(conversation);
            log.debug("✨ 创建新会话: userId={}, targetId={}", userId, targetId);

        } else {
            // 3. 存在，检查是否被删除
            if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                // 如果会话被删除了，重新激活
                conversation.setIsDeleted(0);
                conversation.setUnreadCount(0);
                log.debug("🔄 重新激活已删除的会话: userId={}, targetId={}", userId, targetId);
            }

            // 4. 构建更新条件
            LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ChatConversation::getUserId, userId)
                    .eq(ChatConversation::getTargetId, targetId)
                    .set(ChatConversation::getLastMsgId, msgId)
                    .set(ChatConversation::getLastMsgContent, truncateContent(content))
                    .set(ChatConversation::getLastMsgTime, time)
                    .set(ChatConversation::getIsDeleted, 0);  // 确保未被删除

            // 5. 如果需要增加未读数且未开启免打扰
            if (incrementUnread && unreadDelta > 0) {
                // 检查是否开启了免打扰
                if (conversation.getIsMuted() == null || conversation.getIsMuted() == 0) {
                    wrapper.setSql("unread_count = unread_count + " + unreadDelta);
                } else {
                    log.debug("🔕 用户 {} 对 {} 开启了免打扰，不增加未读数", userId, targetId);
                }
            }

            this.update(wrapper);
            log.debug("🔄 更新会话: userId={}, targetId={}, unreadDelta={}",
                    userId, targetId, incrementUnread ? unreadDelta : 0);
        }
    }

    @Override
    @Transactional
    public void clearUnreadCount(String userId, String targetId) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId)
                .set(ChatConversation::getUnreadCount, 0);

        boolean updated = this.update(wrapper);

        if (updated) {
            log.info("🧹 清空未读数: userId={}, targetId={}", userId, targetId);
        }
    }

    @Override
    @Transactional
    public void toggleTop(String userId, String targetId, boolean isTop) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId)
                .set(ChatConversation::getIsTop, isTop ? 1 : 0);

        boolean updated = this.update(wrapper);

        if (updated) {
            log.info("📌 置顶状态变更: userId={}, targetId={}, isTop={}",
                    userId, targetId, isTop);
        }
    }

    @Override
    @Transactional
    public void toggleMute(String userId, String targetId, boolean isMuted) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId)
                .set(ChatConversation::getIsMuted, isMuted ? 1 : 0);

        boolean updated = this.update(wrapper);

        if (updated) {
            log.info("🔕 免打扰状态变更: userId={}, targetId={}, isMuted={}",
                    userId, targetId, isMuted);
        }
    }

    @Override
    public ChatConversation getConversation(String userId, String targetId) {
        return this.getOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId));
    }

    @Override
    @Transactional
    public void deleteConversation(String userId, String targetId) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId)
                .set(ChatConversation::getIsDeleted, 1)
                .set(ChatConversation::getUnreadCount, 0);  // 同时清空未读数

        boolean updated = this.update(wrapper);

        if (updated) {
            log.info("🗑️ 删除会话: userId={}, targetId={}", userId, targetId);
        }
    }

    @Override
    public Integer getTotalUnreadCount(String userId) {
        List<ChatConversation> conversations = this.getUserConversations(userId);

        return conversations.stream()
                .filter(conv -> conv.getIsMuted() == null || conv.getIsMuted() == 0)  // 不过滤免打扰
                .mapToInt(conv -> conv.getUnreadCount() != null ? conv.getUnreadCount() : 0)
                .sum();
    }

    @Override
    @Transactional
    public void updateConversationTime(String userId, String targetId) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getTargetId, targetId)
                .set(ChatConversation::getLastMsgTime, LocalDateTime.now());

        this.update(wrapper);
    }

    /**
     * 生成会话ID（两个用户ID排序后拼接）
     * 保证两个用户的会话ID一致
     */
    private String generateConversationId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    /**
     * 截断内容预览
     * @param content 原始内容
     * @return 截断后的内容
     */
    private String truncateContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 去除多余的空白字符
        String trimmed = content.replaceAll("\\s+", " ").trim();

        // 截取前50个字符
        if (trimmed.length() > 50) {
            return trimmed.substring(0, 47) + "...";
        }

        return trimmed;
    }
}