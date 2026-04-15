package chat.service;

import chat.domain.pojo.ChatConversation;
import chat.domain.pojo.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 会话服务接口
 */
public interface ChatConversationService extends IService<ChatConversation> {

    /**
     * 获取用户的所有会话列表（按置顶和时间排序）
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatConversation> getUserConversations(String userId);

    /**
     * 更新会话信息（有新消息时调用）
     * @param message 消息实体
     */
    void updateConversation(ChatMessage message);

    /**
     * 清空指定会话的未读消息数
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     */
    void clearUnreadCount(String userId, String targetId);

    /**
     * 置顶或取消置顶会话
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     * @param isTop true-置顶, false-取消置顶
     */
    void toggleTop(String userId, String targetId, boolean isTop);

    /**
     * 免打扰设置
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     * @param isMuted true-开启免打扰, false-关闭免打扰
     */
    void toggleMute(String userId, String targetId, boolean isMuted);

    /**
     * 获取两个用户之间的会话
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     * @return 会话实体，不存在返回null
     */
    ChatConversation getConversation(String userId, String targetId);

    /**
     * 删除会话（软删除，只是隐藏）
     * @param userId 当前用户ID
     * @param targetId 对方用户ID
     */
    void deleteConversation(String userId, String targetId);

    /**
     * 获取用户未读消息总数
     * @param userId 用户ID
     * @return 未读总数
     */
    Integer getTotalUnreadCount(String userId);

    /**
     * 批量更新会话时间（用于会话排序）
     * @param userId 用户ID
     * @param targetId 对方用户ID
     */
    void updateConversationTime(String userId, String targetId);
}